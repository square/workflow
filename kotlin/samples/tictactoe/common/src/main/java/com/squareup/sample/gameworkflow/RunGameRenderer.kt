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
import com.squareup.sample.gameworkflow.RunGameState.GameOver
import com.squareup.sample.gameworkflow.RunGameState.MaybeQuitting
import com.squareup.sample.gameworkflow.RunGameState.NewGame
import com.squareup.sample.gameworkflow.RunGameState.Playing
import com.squareup.viewregistry.AlertScreen
import com.squareup.viewregistry.AlertScreen.Button.NEGATIVE
import com.squareup.viewregistry.AlertScreen.Button.NEUTRAL
import com.squareup.viewregistry.AlertScreen.Button.POSITIVE
import com.squareup.viewregistry.AlertScreen.Event.ButtonClicked
import com.squareup.viewregistry.AlertScreen.Event.Canceled
import com.squareup.viewregistry.EventHandlingScreen.Companion.ignoreEvents
import com.squareup.viewregistry.MainAndModalScreen
import com.squareup.viewregistry.StackedMainAndModalScreen
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.adaptEvents
import com.squareup.workflow.render

object RunGameRenderer :
    Renderer<RunGameState, RunGameEvent, StackedMainAndModalScreen<*, AlertScreen>> {

  override fun render(
    state: RunGameState,
    workflow: WorkflowInput<RunGameEvent>,
    workflows: WorkflowPool
  ): StackedMainAndModalScreen<*, AlertScreen> {
    return when (state) {

      is Playing -> {
        return TakeTurnsRenderer
            .render(state.takingTurns, workflows)
            .let { MainAndModalScreen(it) }
      }

      is NewGame -> StackedMainAndModalScreen(
          NewGameScreen(workflow::sendEvent)
      )

      is MaybeQuitting -> StackedMainAndModalScreen(
          GamePlayScreen(
              state.completedGame.lastTurn, ignoreEvents()
          ),
          AlertScreen(
              workflow.adaptEvents<AlertScreen.Event, RunGameEvent> { alertEvent ->
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
                  POSITIVE to "I Quit",
                  NEGATIVE to "No"
              ),
              message = "Do you really want to concede the game?"
          )
      )

      is GameOver -> StackedMainAndModalScreen(
          GameOverScreen(state, workflow::sendEvent)
      )
    }
  }
}
