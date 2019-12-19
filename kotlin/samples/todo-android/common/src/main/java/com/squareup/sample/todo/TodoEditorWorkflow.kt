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

import com.squareup.sample.todo.TodoAction.GoBackClicked
import com.squareup.sample.todo.TodoAction.ListAction.DeleteClicked
import com.squareup.sample.todo.TodoAction.ListAction.DoneClicked
import com.squareup.sample.todo.TodoAction.ListAction.TextChanged
import com.squareup.sample.todo.TodoAction.ListAction.TitleChanged
import com.squareup.sample.todo.TodoEditorOutput.Done
import com.squareup.sample.todo.TodoEditorOutput.ListUpdated
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater

data class TodoList(
  val title: String,
  val rows: List<TodoRow> = emptyList()
)

data class TodoRow(
  val text: String,
  val done: Boolean = false
)

sealed class TodoAction : WorkflowAction<Nothing, TodoEditorOutput> {
  object GoBackClicked : TodoAction()

  sealed class ListAction : TodoAction() {
    abstract val list: TodoList

    class TitleChanged(
      override val list: TodoList,
      val newTitle: String
    ) : ListAction()

    class DoneClicked(
      override val list: TodoList,
      val index: Int
    ) : ListAction()

    class TextChanged(
      override val list: TodoList,
      val index: Int,
      val newText: String
    ) : ListAction()

    class DeleteClicked(
      override val list: TodoList,
      val index: Int
    ) : ListAction()
  }

  override fun Updater<Nothing, TodoEditorOutput>.apply() {
    when (this@TodoAction) {
      is GoBackClicked -> Done
      is TitleChanged -> ListUpdated(list.copy(title = newTitle))
      is DoneClicked -> ListUpdated(list.updateRow(index) { copy(done = !done) })
      is TextChanged -> ListUpdated(list.updateRow(index) { copy(text = newText) })
      is DeleteClicked -> ListUpdated(list.removeRow(index))
    }.let { setOutput(it) }
  }
}

data class TodoRendering(
  val list: TodoList,
  val onTitleChanged: (title: String) -> Unit,
  val onDoneClicked: (index: Int) -> Unit,
  val onTextChanged: (index: Int, text: String) -> Unit,
  val onDeleteClicked: (index: Int) -> Unit,
  val onGoBackClicked: () -> Unit
)

sealed class TodoEditorOutput {
  data class ListUpdated(val newList: TodoList) : TodoEditorOutput()
  object Done : TodoEditorOutput()
}

class TodoEditorWorkflow : StatelessWorkflow<TodoList, TodoEditorOutput, TodoRendering>() {

  override fun render(
    props: TodoList,
    context: RenderContext<Nothing, TodoEditorOutput>
  ): TodoRendering {
    // Make event handling idempotent until https://github.com/square/workflow/issues/541 is fixed.
    var eventFired = false
    val sink = object : Sink<TodoAction> {
      override fun send(value: TodoAction) {
        if (eventFired) return
        eventFired = true
        context.actionSink.send(value)
      }
    }

    return TodoRendering(
        props.copy(rows = props.rows + TodoRow("")),
        onTitleChanged = { sink.send(TitleChanged(props, it)) },
        onDoneClicked = { sink.send(DoneClicked(props, it)) },
        onTextChanged = { index, newText -> sink.send(TextChanged(props, index, newText)) },
        onDeleteClicked = { sink.send(DeleteClicked(props, it)) },
        onGoBackClicked = { sink.send(GoBackClicked) }
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
