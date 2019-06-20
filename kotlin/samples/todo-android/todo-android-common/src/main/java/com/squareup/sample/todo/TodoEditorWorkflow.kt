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

import com.squareup.sample.todo.TodoEvent.DeleteClicked
import com.squareup.sample.todo.TodoEvent.DoneClicked
import com.squareup.sample.todo.TodoEvent.TextChanged
import com.squareup.sample.todo.TodoEvent.TitleChanged
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState

data class TodoList(
  val title: String,
  val rows: List<TodoRow> = emptyList()
)

data class TodoRow(
  val text: String,
  val done: Boolean = false
)

data class TodoRendering(
  val list: TodoList,
  val onEvent: (TodoEvent) -> Unit
)

sealed class TodoEvent {
  data class TitleChanged(val title: String) : TodoEvent()
  data class DoneClicked(val index: Int) : TodoEvent()
  data class TextChanged(
    val index: Int,
    val text: String
  ) : TodoEvent()

  data class DeleteClicked(val index: Int) : TodoEvent()
}

class TodoEditorWorkflow : StatefulWorkflow<Unit, TodoList, Nothing, TodoRendering>() {

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): TodoList = TodoList(title = "Groceries", rows = listOf(TodoRow("Potatoes")))

  override fun render(
    input: Unit,
    state: TodoList,
    context: RenderContext<TodoList, Nothing>
  ): TodoRendering {
    return TodoRendering(
        list = state.copy(rows = state.rows + TodoRow("")),
        onEvent = context.onEvent {
          println("got event: $it")
          when (it) {
            is TitleChanged -> enterState(state.copy(title = it.title))
            is DoneClicked -> enterState(state.updateRow(it.index) {
              copy(done = !done)
            })
            is TextChanged -> enterState(state.updateRow(it.index) {
              copy(text = it.text)
            })
            is DeleteClicked -> enterState(state.removeRow(it.index))
          }
        }
    )
  }

  override fun snapshotState(state: TodoList): Snapshot = Snapshot.EMPTY
}

private fun TodoList.updateRow(
  index: Int,
  block: TodoRow.() -> TodoRow
) = copy(rows = if (index == rows.size) {
  rows + TodoRow("").block()
} else {
  rows.withIndex()
      .map { (i, value) ->
        if (i == index) value.block() else value
      }
})

private fun TodoList.removeRow(index: Int) = copy(rows = rows.filterIndexed { i, _ -> i != index })
