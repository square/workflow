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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.sample.todo.R
import com.squareup.sample.todo.TodoList
import com.squareup.sample.todo.TodoListsRendering
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

@UseExperimental(ExperimentalWorkflowUi::class)
internal class TodoListsLayoutRunner(view: View) : LayoutRunner<TodoListsRendering> {
  private val inflater = LayoutInflater.from(view.context)
  private val listsContainer = view.findViewById<ViewGroup>(R.id.todo_lists_container)

  override fun showRendering(rendering: TodoListsRendering) {
    for ((index, list) in rendering.lists.withIndex()) {
      addRow(index, list) { rendering.onRowClicked(index) }
    }
    pruneDeadRowsFrom(rendering.lists.size)
  }

  private fun addRow(
    index: Int,
    list: TodoList,
    onClick: () -> Unit
  ) {
    val row: TextView = if (index < listsContainer.childCount) {
      listsContainer.getChildAt(index)
    } else {
      inflater.inflate(R.layout.todo_lists_row_layout, listsContainer, false)
          .also { listsContainer.addView(it) }
    } as TextView

    row.text = list.title
    row.setOnClickListener { onClick() }
  }

  private fun pruneDeadRowsFrom(index: Int) {
    while (listsContainer.childCount > index) listsContainer.removeViewAt(index)
  }

  companion object : ViewBinding<TodoListsRendering> by bind(
      R.layout.todo_lists_layout, ::TodoListsLayoutRunner
  )
}
