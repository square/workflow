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
package com.squareup.sample.mainworkflow

import com.squareup.sample.authworkflow.AuthRenderer
import com.squareup.sample.gameworkflow.GamePlayScreen
import com.squareup.sample.gameworkflow.RunGameRenderer
import com.squareup.sample.mainworkflow.MainState.Authenticating
import com.squareup.sample.mainworkflow.MainState.RunningGame
import com.squareup.sample.panel.PanelContainerScreen
import com.squareup.sample.panel.asPanelOver
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.viewregistry.BackStackScreen
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.render

typealias RootScreen = AlertContainerScreen<PanelContainerScreen<*, *>>

object MainRenderer : Renderer<MainState, Nothing, RootScreen> {
  override fun render(
    state: MainState,
    workflow: WorkflowInput<Nothing>,
    workflows: WorkflowPool
  ): RootScreen {
    return when (state) {
      is Authenticating -> {
        val authScreen: BackStackScreen<*> = AuthRenderer.render(state.authWorkflow, workflows)
        val emptyGameScreen = GamePlayScreen()

        AlertContainerScreen(authScreen.asPanelOver(emptyGameScreen))
      }

      is RunningGame -> {
        val (baseScreen, alerts) = RunGameRenderer.render(state.runGameWorkflow, workflows)
        AlertContainerScreen(baseScreen, alerts)
      }
    }
  }
}
