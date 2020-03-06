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

import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.squareup.sample.todo.R
import com.squareup.sample.todo.TodoRendering
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.backPressedHandler
import com.squareup.workflow.ui.backstack.BackStackConfig
import com.squareup.workflow.ui.backstack.BackStackConfig.Other

internal class TodoEditorLayoutRunner(private val view: View) : LayoutRunner<TodoRendering> {

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

  override fun showRendering(
    rendering: TodoRendering,
    viewEnvironment: ViewEnvironment
  ) {
    toolbar.title = rendering.list.title
    titleText.text.replace(0, titleText.text.length, rendering.list.title)
    itemContainer.setRows(rendering.list.rows.map { Pair(it.done, it.text) })

    if (viewEnvironment[BackStackConfig] == Other) {
      toolbar.setNavigationOnClickListener { rendering.onGoBackClicked() }
      view.backPressedHandler = { rendering.onGoBackClicked() }
    } else {
      toolbar.navigationIcon = null
    }

    titleText.setTextChangedListener { rendering.onTitleChanged(it) }

    itemContainer.onDoneClickedListener = { index ->
      rendering.onDoneClicked(index)
    }
    itemContainer.onTextChangedListener = { index, text ->
      rendering.onTextChanged(index, text)
    }
    itemContainer.onDeleteClickedListener = { index ->
      rendering.onDeleteClicked(index)
    }
  }

  companion object : ViewBinding<TodoRendering> by bind(
      R.layout.todo_editor_layout, ::TodoEditorLayoutRunner
  )
}
