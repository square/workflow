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
package com.squareup.sample.recyclerview

import android.view.View
import android.widget.Button
import com.squareup.sample.recyclerview.AppWorkflow.BaseScreen
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.Asynchronous
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.Synchronous
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.WorkflowViewStub

/**
 * Renders a [BaseScreen] from the [AppWorkflow] by showing the list in three separate
 * `RecyclerView`s at once, all bound to the same rendering.
 *
 * Each of the `RecyclerView`s uses a different [ListDiffMode] for updating its adapter.
 */
class BaseScreenLayoutRunner(view: View) : LayoutRunner<BaseScreen> {

  private val noDiffListStub = view.findViewById<WorkflowViewStub>(R.id.list_stub)
  private val syncListStub = view.findViewById<WorkflowViewStub>(R.id.sync_list_stub)
  private val asyncListStub = view.findViewById<WorkflowViewStub>(R.id.async_list_stub)
  private val addRowButton = view.findViewById<Button>(R.id.add_new_row_button)

  override fun showRendering(
    rendering: BaseScreen,
    viewEnvironment: ViewEnvironment
  ) {
    val syncHints = viewEnvironment + (ListDiffMode to Synchronous)
    val asyncHints = viewEnvironment + (ListDiffMode to Asynchronous)
    noDiffListStub.update(rendering.listRendering, viewEnvironment)
    syncListStub.update(rendering.listRendering, syncHints)
    asyncListStub.update(rendering.listRendering, asyncHints)
    addRowButton.setOnClickListener { rendering.onAddRowTapped() }
  }

  companion object : ViewBinding<BaseScreen> by bind(
      R.layout.base_screen_layout, ::BaseScreenLayoutRunner
  )
}
