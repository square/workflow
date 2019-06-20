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

import com.squareup.sample.todo.TodoListsAppState.EditingList
import com.squareup.sample.todo.TodoListsAppState.ShowingLists
import com.squareup.sample.todo.TodoEditorOutput.Done
import com.squareup.sample.todo.TodoEditorOutput.ListUpdated
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.ui.BackStackScreen

sealed class TodoListsAppState {
  abstract val lists: List<TodoList>

  data class ShowingLists(override val lists: List<TodoList>) : TodoListsAppState()

  data class EditingList(
    override val lists: List<TodoList>,
    val editingIndex: Int
  ) : TodoListsAppState()
}

class TodoListsAppWorkflow :
    StatefulWorkflow<Unit, TodoListsAppState, Nothing, BackStackScreen<*>>() {
  override fun initialState(
    input: Unit,
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

  override fun render(
    input: Unit,
    state: TodoListsAppState,
    context: RenderContext<TodoListsAppState, Nothing>
  ): BackStackScreen<*> {
    val listOfLists = context.renderChild(
        listsWorkflow,
        state.lists
    ) { index -> enterState(EditingList(state.lists, index)) }

    return when (state) {
      is ShowingLists -> BackStackScreen(listOfLists)
      is EditingList -> context.renderChild(
          editorWorkflow, state.lists[state.editingIndex]
      ) { result ->
        when (result) {
          is ListUpdated -> enterState(
              state.copy(lists = state.lists.updateRow(state.editingIndex, result.newList))
          )
          Done -> enterState(ShowingLists(state.lists))
        }
      }.let { editScreen ->
        BackStackScreen(listOfLists, editScreen)
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
