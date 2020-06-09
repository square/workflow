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
import android.widget.TextView
import com.squareup.sample.container.overviewdetail.OverviewDetailConfig
import com.squareup.sample.container.overviewdetail.OverviewDetailConfig.Overview
import com.squareup.sample.todo.R
import com.squareup.sample.todo.TodoList
import com.squareup.sample.todo.TodoListsScreen
import com.squareup.sample.todo.databinding.TodoListsLayoutBinding
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewFactory

internal val TodoListsViewFactory: ViewFactory<TodoListsScreen> =
  LayoutRunner.bind(TodoListsLayoutBinding::inflate) { rendering, containerHints ->
    for ((index, list) in rendering.lists.withIndex()) {
      addRow(
          index,
          list,
          selectable = containerHints[OverviewDetailConfig] == Overview,
          selected = index == rendering.selection && containerHints[OverviewDetailConfig] == Overview
      ) { rendering.onRowClicked(index) }
    }
    pruneDeadRowsFrom(rendering.lists.size)
  }

private fun TodoListsLayoutBinding.addRow(
  index: Int,
  list: TodoList,
  selectable: Boolean,
  selected: Boolean,
  onClick: () -> Unit
) {
  val row: TextView = if (index < todoListsContainer.childCount) {
    todoListsContainer.getChildAt(index)
  } else {
    val layout = when {
      selectable -> R.layout.todo_lists_selectable_row_layout
      else -> R.layout.todo_lists_unselectable_row_layout
    }
    LayoutInflater.from(root.context)
        .inflate(layout, todoListsContainer, false)
        .also { todoListsContainer.addView(it) }
  } as TextView

  row.isActivated = selected
  row.text = list.title
  row.setOnClickListener { onClick() }
}

private fun TodoListsLayoutBinding.pruneDeadRowsFrom(index: Int) {
  while (todoListsContainer.childCount > index) todoListsContainer.removeViewAt(index)
}
