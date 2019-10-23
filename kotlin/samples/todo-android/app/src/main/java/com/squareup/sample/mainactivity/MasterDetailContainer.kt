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
import com.squareup.sample.todo.MasterDetailAware.Companion.makeAware
import com.squareup.sample.todo.MasterDetailConfig.Detail
import com.squareup.sample.todo.MasterDetailConfig.Master
import com.squareup.sample.todo.MasterDetailConfig.Only
import com.squareup.sample.todo.MasterDetailScreen
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowViewStub

/**
 * Displays [MasterDetailScreen] renderings in either split pane or single pane
 * treatment, depending on the setup of the given [View]. The view must provide
 * either a single [WorkflowViewStub] with id [R.id.master_detail_single_stub],
 * or else two with ids [R.id.master_stub] and [R.id.detail_stub].
 *
 * For single pane layouts, [MasterDetailScreen] is repackaged as a [BackStackScreen]
 * with [MasterDetailScreen.masterRendering] as the base of the stack.
 */
class MasterDetailContainer(
  view: View,
  private val registry: ViewRegistry
) : LayoutRunner<MasterDetailScreen> {

  private val masterStub: WorkflowViewStub? = view.findViewById(R.id.master_stub)
  private val detailStub: WorkflowViewStub? = view.findViewById(R.id.detail_stub)
  private val singleStub: WorkflowViewStub? = view.findViewById(R.id.master_detail_single_stub)

  init {
    check((singleStub == null) xor (masterStub == null && detailStub == null)) {
      "Layout must define only R.id.master_detail_single_stub, " +
          "or else both R.id.master_stub and R.id.detail_stub"
    }
  }

  override fun showRendering(rendering: MasterDetailScreen) {
    if (singleStub == null) renderSplitView(rendering)
    else renderSingleView(rendering, singleStub)
  }

  private fun renderSplitView(rendering: MasterDetailScreen) {
    if (rendering.detailRendering == null && rendering.selectDefault != null) {
      rendering.selectDefault!!.invoke()
    } else {
      val aware = rendering.copy(
          masterRendering = makeAware(rendering.masterRendering, Master),
          detailRendering = makeAware(rendering.detailRendering, Detail)
      )

      masterStub!!.update(aware.masterRendering, registry)
      aware.detailRendering?.let { detailStub!!.update(it, registry) }
    }
  }

  private fun renderSingleView(
    rendering: MasterDetailScreen,
    stub: WorkflowViewStub
  ) {
    val asBackStack: BackStackScreen<Any> = rendering.detailRendering
        ?.let { BackStackScreen(rendering.masterRendering, makeAware(it, Only)) }
        ?: run { BackStackScreen(makeAware(rendering.masterRendering, Only)) }

    stub.update(asBackStack, registry)
  }

  companion object : ViewBinding<MasterDetailScreen> by LayoutRunner.Binding(
      type = MasterDetailScreen::class,
      layoutId = R.layout.master_detail,
      runnerConstructor = ::MasterDetailContainer
  )
}
