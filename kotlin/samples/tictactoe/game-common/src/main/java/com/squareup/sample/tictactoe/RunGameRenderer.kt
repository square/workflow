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
package com.squareup.sample.tictactoe

import com.squareup.sample.tictactoe.RunGameEvent.ConfirmQuit
import com.squareup.sample.tictactoe.RunGameEvent.ContinuePlaying
import com.squareup.sample.tictactoe.RunGameState.MaybeQuitting
import com.squareup.sample.tictactoe.RunGameState.Playing
import com.squareup.viewregistry.Alert
import com.squareup.viewregistry.Alert.Button.NEGATIVE
import com.squareup.viewregistry.Alert.Button.NEUTRAL
import com.squareup.viewregistry.Alert.Button.POSITIVE
import com.squareup.viewregistry.Alert.Event.ButtonClicked
import com.squareup.viewregistry.Alert.Event.Canceled
import com.squareup.viewregistry.EventHandlingScreen.Companion.ignoreEvents
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.adaptEvents
import com.squareup.workflow.render

object RunGameRenderer :
    Renderer<RunGameState, RunGameEvent, AlertContainerScreen<*>> {

  override fun render(
    state: RunGameState,
    workflow: WorkflowInput<RunGameEvent>,
    workflows: WorkflowPool
  ): AlertContainerScreen<*> {
    return when (state) {

      is Playing -> {
        return TakeTurnsRenderer
            .render(state.takingTurns, workflows)
            .let { AlertContainerScreen(it) }
      }

      is RunGameState.NewGame -> AlertContainerScreen(NewGameScreen(workflow::sendEvent))

      is MaybeQuitting -> AlertContainerScreen(
          GamePlayScreen(state.completedGame.lastTurn, ignoreEvents()),
          Alert(
              workflow.adaptEvents<Alert.Event, RunGameEvent> { alertEvent ->
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

      is RunGameState.GameOver -> AlertContainerScreen(
          GameOverScreen(state, workflow::sendEvent)
      )
    }
  }
}
