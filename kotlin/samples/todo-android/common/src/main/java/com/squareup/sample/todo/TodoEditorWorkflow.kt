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

import com.squareup.sample.todo.TodoEditorOutput.Done
import com.squareup.sample.todo.TodoEditorOutput.ListUpdated
import com.squareup.sample.todo.TodoEvent.DeleteClicked
import com.squareup.sample.todo.TodoEvent.DoneClicked
import com.squareup.sample.todo.TodoEvent.GoBackClicked
import com.squareup.sample.todo.TodoEvent.TextChanged
import com.squareup.sample.todo.TodoEvent.TitleChanged
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput

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

sealed class TodoEditorOutput {
  data class ListUpdated(val newList: TodoList) : TodoEditorOutput()
  object Done : TodoEditorOutput()
}

sealed class TodoEvent {
  data class TitleChanged(val title: String) : TodoEvent()
  data class DoneClicked(val index: Int) : TodoEvent()
  data class TextChanged(
    val index: Int,
    val text: String
  ) : TodoEvent()

  data class DeleteClicked(val index: Int) : TodoEvent()
  object GoBackClicked : TodoEvent()
}

class TodoEditorWorkflow : StatelessWorkflow<TodoList, TodoEditorOutput, TodoRendering>() {

  override fun render(
    input: TodoList,
    context: RenderContext<Nothing, TodoEditorOutput>
  ): TodoRendering {
    return TodoRendering(
        list = input.copy(rows = input.rows + TodoRow("")),
        onEvent = context.onEvent {
          println("got event: $it")
          when (it) {
            is GoBackClicked -> emitOutput(Done)
            is TitleChanged -> emitOutput(ListUpdated(input.copy(title = it.title)))
            is DoneClicked -> emitOutput(ListUpdated(input.updateRow(it.index) {
              copy(done = !done)
            }))
            is TextChanged -> emitOutput(ListUpdated(input.updateRow(it.index) {
              copy(text = it.text)
            }))
            is DeleteClicked -> emitOutput(ListUpdated(input.removeRow(it.index)))
          }
        }
    )
  }
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
