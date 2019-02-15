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
package com.squareup.sample.gameworkflow

import com.squareup.sample.gameworkflow.RunGameEvent.ConfirmQuit
import com.squareup.sample.gameworkflow.RunGameEvent.ContinuePlaying
import com.squareup.sample.gameworkflow.RunGameState.MaybeQuitting
import com.squareup.sample.gameworkflow.RunGameState.MaybeQuittingForSure
import com.squareup.sample.gameworkflow.RunGameState.Playing
import com.squareup.sample.panel.PanelContainerScreen
import com.squareup.sample.panel.asPanelOver
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.viewregistry.AlertScreen
import com.squareup.viewregistry.AlertScreen.Button.NEGATIVE
import com.squareup.viewregistry.AlertScreen.Button.NEUTRAL
import com.squareup.viewregistry.AlertScreen.Button.POSITIVE
import com.squareup.viewregistry.AlertScreen.Event
import com.squareup.viewregistry.AlertScreen.Event.ButtonClicked
import com.squareup.viewregistry.AlertScreen.Event.Canceled
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.adaptEvents
import com.squareup.workflow.render

object RunGameRenderer :
    Renderer<RunGameState, RunGameEvent, AlertContainerScreen<PanelContainerScreen<*, *>>> {

  override fun render(
    state: RunGameState,
    workflow: WorkflowInput<RunGameEvent>,
    workflows: WorkflowPool
  ): AlertContainerScreen<PanelContainerScreen<*, *>> {
    return when (state) {

      is Playing -> simpleScreen(TakeTurnsRenderer.render(state.takingTurns, workflows))

      is RunGameState.NewGame -> {
        val emptyGameScreen = GamePlayScreen()
        subflowScreen(
            base = emptyGameScreen,
            subflow = NewGameScreen(
                state.defaultXName,
                state.defaultOName,
                workflow::sendEvent
            )
        )
      }

      is MaybeQuitting -> alertScreen(
          base = GamePlayScreen(state.completedGame.lastTurn),
          alert = maybeQuitScreen(workflow)
      )

      is MaybeQuittingForSure -> nestedAlertsScreen(
          GamePlayScreen(state.completedGame.lastTurn),
          maybeQuitScreen(workflow),
          maybeQuitScreen(workflow, "Really?", "Yes!!", "Sigh, no")
      )

      is RunGameState.GameOver -> simpleScreen(GameOverScreen(state, workflow::sendEvent))
    }
  }

  private fun nestedAlertsScreen(
    base: Any,
    vararg alerts: AlertScreen
  ): AlertContainerScreen<PanelContainerScreen<*, *>> {
    return AlertContainerScreen(PanelContainerScreen<Any, Any>(base), *alerts)
  }

  private fun alertScreen(
    base: Any,
    alert: AlertScreen
  ): AlertContainerScreen<PanelContainerScreen<*, *>> {
    return AlertContainerScreen(PanelContainerScreen<Any, Any>(base), alert)
  }

  private fun subflowScreen(
    base: Any,
    subflow: Any
  ): AlertContainerScreen<PanelContainerScreen<*, *>> {
    return AlertContainerScreen(subflow.asPanelOver(base))
  }

  private fun simpleScreen(screen: Any): AlertContainerScreen<PanelContainerScreen<*, *>> {
    return AlertContainerScreen(PanelContainerScreen<Any, Any>(screen))
  }

  private fun maybeQuitScreen(
    workflow: WorkflowInput<RunGameEvent>,
    message: String = "Do you really want to concede the game?",
    positive: String = "I Quit",
    negative: String = "No"
  ): AlertScreen {
    return AlertScreen(
        workflow.adaptEvents<Event, RunGameEvent> { alertEvent ->
          when (alertEvent) {
            is ButtonClicked -> when (alertEvent.button) {
              POSITIVE -> ConfirmQuit
              NEGATIVE -> ContinuePlaying
              NEUTRAL -> throw IllegalArgumentException()
            }
            Canceled -> ContinuePlaying
          }
        }::sendEvent,
        buttons = mapOf(
            POSITIVE to positive,
            NEGATIVE to negative
        ),
        message = message
    )
  }
}
