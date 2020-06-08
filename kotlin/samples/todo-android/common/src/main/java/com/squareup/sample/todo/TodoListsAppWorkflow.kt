/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.todo

import com.squareup.sample.container.overviewdetail.OverviewDetailScreen
import com.squareup.sample.todo.TodoEditorOutput.Done
import com.squareup.sample.todo.TodoEditorOutput.ListUpdated
import com.squareup.sample.todo.TodoListsAppState.EditingList
import com.squareup.sample.todo.TodoListsAppState.ShowingLists
import com.squareup.sample.todo.TodoListsAppWorkflow.render
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.action
import com.squareup.workflow.ui.backstack.BackStackScreen

private typealias TodoListsAction = WorkflowAction<TodoListsAppState, Nothing>

sealed class TodoListsAppState {
  abstract val lists: List<TodoList>

  data class ShowingLists(override val lists: List<TodoList>) : TodoListsAppState()

  data class EditingList(
    override val lists: List<TodoList>,
    val editingIndex: Int
  ) : TodoListsAppState()
}

/**
 * Renders a [TodoListsWorkflow] and a [TodoEditorWorkflow] in a overview / detail
 * relationship. See details in the body of the [render] method.
 */
object TodoListsAppWorkflow :
    StatefulWorkflow<Unit, TodoListsAppState, Nothing, OverviewDetailScreen>() {
  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): TodoListsAppState = ShowingLists(
      listOf(
          TodoList("Groceries"),
          TodoList("Daily Chores"),
          TodoList("Reminders")
      )
  )

  private val listsWorkflow = TodoListsWorkflow()
  private val editorWorkflow = TodoEditorWorkflow()

  private fun onListSelected(index: Int): TodoListsAction = action {
    nextState = EditingList(nextState.lists, index)
  }

  private fun onEditOutput(output: TodoEditorOutput): TodoListsAction = action {
    nextState = when (output) {
      is ListUpdated -> {
        val oldState = nextState as EditingList
        oldState.copy(
            lists = nextState.lists.updateRow(oldState.editingIndex, output.newList)
        )
      }
      Done -> ShowingLists(nextState.lists)
    }
  }

  override fun render(
    props: Unit,
    state: TodoListsAppState,
    context: RenderContext<TodoListsAppState, Nothing>
  ): OverviewDetailScreen {
    val listOfLists: TodoListsScreen = context.renderChild(
        listsWorkflow,
        state.lists
    ) { index -> onListSelected(index) }

    return when (state) {
      // Nothing is selected. We rest in this state on a phone in portrait orientation.
      // In a overview detail layout, selectDefault can be called immediately, so that
      // the detail panel is never seen to be empty.
      is ShowingLists -> OverviewDetailScreen(
          overviewRendering = BackStackScreen(listOfLists),
          selectDefault = { context.actionSink.send(onListSelected(0)) }
      )

      // We are editing a list. Notice that we always render the overview pane -- the
      // workflow has no knowledge of whether the view side is running in a single
      // pane config or as a overview / detail split view.
      //
      // Also notice that we update the TodoListsScreen rendering that we got from the
      // TodoListsWorkflow child to reflect the current selection. The child workflow has no
      // notion of selection, and leaves that field set to the default value of -1.

      is EditingList -> context.renderChild(
          editorWorkflow, state.lists[state.editingIndex], handler = this::onEditOutput
      ).let { editScreen ->
        OverviewDetailScreen(
            overviewRendering = BackStackScreen(listOfLists.copy(selection = state.editingIndex)),
            detailRendering = BackStackScreen(editScreen)
        )
      }
    }
  }

  override fun snapshotState(state: TodoListsAppState): Snapshot = Snapshot.EMPTY
}

private fun <T> List<T>.updateRow(
  index: Int,
  newValue: T
): List<T> = withIndex().map { (i, value) ->
  if (i == index) newValue else value
}
