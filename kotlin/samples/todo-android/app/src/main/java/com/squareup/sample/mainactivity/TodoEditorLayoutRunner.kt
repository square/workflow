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
import com.squareup.sample.todo.TodoRendering
import com.squareup.sample.todo.databinding.TodoEditorLayoutBinding
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.backPressedHandler
import com.squareup.workflow.ui.backstack.BackStackConfig
import com.squareup.workflow.ui.backstack.BackStackConfig.Other

internal class TodoEditorLayoutRunner(
  private val binding: TodoEditorLayoutBinding
) : LayoutRunner<TodoRendering> {

  private val itemListView = ItemListView.fromLinearLayout(binding.itemContainer)

  init {
    with(binding) {
      todoEditorToolbar.setOnClickListener {
        todoTitle.visibility = View.VISIBLE
        todoTitle.requestFocus()
        todoTitle.showSoftKeyboard()
      }

      @Suppress("UsePropertyAccessSyntax")
      todoTitle.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) todoTitle.visibility = View.GONE
      }
    }
  }

  override fun showRendering(
    rendering: TodoRendering,
    viewEnvironment: ViewEnvironment
  ) {
    with(binding) {
      todoEditorToolbar.title = rendering.list.title
      todoTitle.text.replace(0, todoTitle.text.length, rendering.list.title)
      itemListView.setRows(rendering.list.rows.map { Pair(it.done, it.text) })

      if (viewEnvironment[BackStackConfig] == Other) {
        todoEditorToolbar.setNavigationOnClickListener { rendering.onGoBackClicked() }
        root.backPressedHandler = { rendering.onGoBackClicked() }
      } else {
        todoEditorToolbar.navigationIcon = null
      }

      todoTitle.setTextChangedListener { rendering.onTitleChanged(it) }

      itemListView.onDoneClickedListener = { index ->
        rendering.onDoneClicked(index)
      }
      itemListView.onTextChangedListener = { index, text ->
        rendering.onTextChanged(index, text)
      }
      itemListView.onDeleteClickedListener = { index ->
        rendering.onDeleteClicked(index)
      }
    }
  }

  companion object : ViewFactory<TodoRendering> by bind(
      TodoEditorLayoutBinding::inflate, ::TodoEditorLayoutRunner
  )
}
