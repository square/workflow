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
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

@UseExperimental(ExperimentalWorkflowUi::class)
internal class TodoEditorLayoutRunner(view: View) : LayoutRunner<Unit> {

  private val toolbar = view.findViewById<Toolbar>(R.id.todo_editor_toolbar)
  private val itemContainer = ItemListView.fromLinearLayout(view, R.id.item_container)

  override fun showRendering(rendering: Unit) {
    toolbar.title = "Groceries"
    itemContainer.setRows(listOf(Pair(false, "Potatoes")))
  }

  companion object : ViewBinding<Unit> by bind(
      R.layout.todo_editor_layout, ::TodoEditorLayoutRunner
  )
}
