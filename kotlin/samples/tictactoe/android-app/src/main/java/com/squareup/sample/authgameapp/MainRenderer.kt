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
package com.squareup.sample.authgameapp

import com.squareup.sample.authgameapp.MainState.Authenticating
import com.squareup.sample.authgameapp.MainState.RunningGame
import com.squareup.sample.authworkflow.AuthRenderer
import com.squareup.sample.tictactoe.RunGameRenderer
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.viewregistry.toAlertContainerScreen
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.render

object MainRenderer :
    Renderer<MainState, Nothing, AlertContainerScreen<*>> {
  override fun render(
    state: MainState,
    workflow: WorkflowInput<Nothing>,
    workflows: WorkflowPool
  ): AlertContainerScreen<*> {
    return when (state) {
      is Authenticating ->
        AuthRenderer.render(state.authWorkflow, workflows).toAlertContainerScreen()

      is RunningGame -> RunGameRenderer.render(state.runGameWorkflow, workflows)
    }
  }
}
