/*
 * Copyright 2017 Square Inc.
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

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.sample.authworkflow.AuthViewBindings
import com.squareup.sample.gameworkflow.TicTacToeViewBindings
import com.squareup.sample.panel.PanelContainer
import com.squareup.workflow.ui.ModalContainer
import com.squareup.workflow.ui.PickledWorkflow
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowActivityRunner
import com.squareup.workflow.ui.backstack.BackStackContainer
import com.squareup.workflow.ui.backstack.PushPopEffect
import com.squareup.workflow.ui.setContentWorkflow

class MainActivity : AppCompatActivity() {
  private lateinit var component: MainComponent
  private lateinit var workflowViewModel: WorkflowActivityRunner<*, *>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val restored = savedInstanceState?.getParcelable<PickledWorkflow>(WORKFLOW_BUNDLE_KEY)
    component = lastCustomNonConfigurationInstance as? MainComponent
        ?: MainComponent()

    workflowViewModel = setContentWorkflow(viewRegistry, component.mainWorkflow, restored)
  }

  override fun onBackPressed() {
    if (!workflowViewModel.onBackPressed(this)) super.onBackPressed()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(WORKFLOW_BUNDLE_KEY, workflowViewModel.asParcelable())
  }

  override fun onRetainCustomNonConfigurationInstance(): Any = component

  private companion object {
    const val WORKFLOW_BUNDLE_KEY = "workflow"

    val viewRegistry = ViewRegistry(
        BackStackContainer,
        ModalContainer.forAlertContainerScreen(),
        PanelContainer
    ) + AuthViewBindings + TicTacToeViewBindings + PushPopEffect
  }
}
