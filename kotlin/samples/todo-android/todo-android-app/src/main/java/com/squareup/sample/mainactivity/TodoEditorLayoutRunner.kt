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
package com.squareup.sample.mainactivity

import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.EditText
import com.squareup.sample.todo.R
import com.squareup.sample.todo.TodoEvent
import com.squareup.sample.todo.TodoEvent.DeleteClicked
import com.squareup.sample.todo.TodoEvent.DoneClicked
import com.squareup.sample.todo.TodoEvent.TextChanged
import com.squareup.sample.todo.TodoEvent.TitleChanged
import com.squareup.sample.todo.TodoRendering
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

@UseExperimental(ExperimentalWorkflowUi::class)
internal class TodoEditorLayoutRunner(view: View) : LayoutRunner<TodoRendering> {

  private val toolbar = view.findViewById<Toolbar>(R.id.todo_editor_toolbar)
  private val titleText = view.findViewById<EditText>(R.id.todo_title)
  private val itemContainer = ItemListView.fromLinearLayout(view, R.id.item_container)

  init {
    toolbar.setOnClickListener {
      titleText.visibility = View.VISIBLE
      titleText.requestFocus()
      titleText.showSoftKeyboard()
    }

    @Suppress("UsePropertyAccessSyntax")
    titleText.setOnFocusChangeListener { _, hasFocus ->
      if (!hasFocus) titleText.visibility = View.GONE
    }
  }

  override fun showRendering(rendering: TodoRendering) {
    toolbar.title = rendering.list.title
    titleText.text.replace(0, titleText.text.length, rendering.list.title)
    itemContainer.setRows(rendering.list.rows.map { Pair(it.done, it.text) })

    // Make event handling idempotent.
    var eventFired = false
    fun onEvent(event: TodoEvent) {
      if (eventFired) return
      eventFired = true
      rendering.onEvent(event)
    }

    titleText.setTextChangedListener { onEvent(TitleChanged(it)) }

    itemContainer.onDoneClickedListener = { index ->
      onEvent(DoneClicked(index))
    }
    itemContainer.onTextChangedListener = { index, text ->
      onEvent(TextChanged(index, text))
    }
    itemContainer.onDeleteClickedListener = { index ->
      onEvent(DeleteClicked(index))
    }
  }

  companion object : ViewBinding<TodoRendering> by bind(
      R.layout.todo_editor_layout, ::TodoEditorLayoutRunner
  )
}
