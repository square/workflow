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

import com.squareup.sample.authworkflow.AuthWorkflow
import com.squareup.sample.gameworkflow.GamePlayScreen
import com.squareup.sample.gameworkflow.RealRunGameWorkflow
import com.squareup.sample.gameworkflow.RunGameWorkflow
import com.squareup.sample.mainworkflow.MainState.Authenticating
import com.squareup.sample.mainworkflow.MainState.RunningGame
import com.squareup.sample.panel.PanelContainerScreen
import com.squareup.sample.panel.asPanelOver
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.compose

typealias RootScreen = AlertContainerScreen<PanelContainerScreen<*, *>>

/**
 * Application specific root [Workflow], and demonstration of workflow composition.
 * We log in, and then play as many games as we want.
 *
 * Delegates to [AuthWorkflow] and [RealRunGameWorkflow]. Responsible only for deciding
 * what to do as each nested workflow ends.
 */
class MainWorkflow(
  private val authWorkflow: AuthWorkflow,
  private val runGameWorkflow: RunGameWorkflow
) : Workflow<Unit, MainState, Unit, RootScreen> {

  override fun initialState(input: Unit): MainState = Authenticating

  override fun compose(
    input: Unit,
    state: MainState,
    context: WorkflowContext<MainState, Unit>
  ): RootScreen = when (state) {
    is Authenticating -> {
      val authScreen = context.compose(authWorkflow) { enterState(RunningGame) }
      val emptyGameScreen = GamePlayScreen()

      AlertContainerScreen(authScreen.asPanelOver(emptyGameScreen))
    }

    is RunningGame -> context.compose(runGameWorkflow) { enterState(Authenticating) }
  }

  override fun snapshotState(state: MainState): Snapshot = state.toSnapshot()

  override fun restoreState(snapshot: Snapshot): MainState {
    return MainState.fromSnapshot(snapshot.bytes)
  }
}
