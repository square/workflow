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

import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.GameLog.LogResult.LOGGED
import com.squareup.sample.gameworkflow.GameLog.LogResult.TRY_LATER
import com.squareup.sample.gameworkflow.GameOverScreen.Event.Exit
import com.squareup.sample.gameworkflow.GameOverScreen.Event.PlayAgain
import com.squareup.sample.gameworkflow.GameOverScreen.Event.TrySaveAgain
import com.squareup.sample.gameworkflow.NewGameScreen.Event.CancelNewGame
import com.squareup.sample.gameworkflow.NewGameScreen.Event.StartGame
import com.squareup.sample.gameworkflow.RunGameResult.CanceledStart
import com.squareup.sample.gameworkflow.RunGameResult.FinishedPlaying
import com.squareup.sample.gameworkflow.RunGameState.GameOver
import com.squareup.sample.gameworkflow.RunGameState.MaybeQuitting
import com.squareup.sample.gameworkflow.RunGameState.MaybeQuittingForSure
import com.squareup.sample.gameworkflow.RunGameState.NewGame
import com.squareup.sample.gameworkflow.RunGameState.Playing
import com.squareup.sample.gameworkflow.SyncState.SAVED
import com.squareup.sample.gameworkflow.SyncState.SAVE_FAILED
import com.squareup.sample.gameworkflow.SyncState.SAVING
import com.squareup.sample.panel.PanelContainerScreen
import com.squareup.sample.panel.asPanelOver
import com.squareup.workflow.ui.AlertContainerScreen
import com.squareup.workflow.ui.AlertScreen
import com.squareup.workflow.ui.AlertScreen.Button.NEGATIVE
import com.squareup.workflow.ui.AlertScreen.Button.NEUTRAL
import com.squareup.workflow.ui.AlertScreen.Button.POSITIVE
import com.squareup.workflow.ui.AlertScreen.Event.ButtonClicked
import com.squareup.workflow.ui.AlertScreen.Event.Canceled
import com.squareup.workflow.EventHandler
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.invoke
import com.squareup.workflow.rx2.onSuccess

enum class RunGameResult {
  CanceledStart,
  FinishedPlaying
}

typealias RunGameScreen = AlertContainerScreen<PanelContainerScreen<*, *>>

/**
 * We define this otherwise redundant typealias to keep composite workflows
 * that build on [RunGameWorkflow] decoupled from it, for ease of testing.
 */
typealias RunGameWorkflow = Workflow<Unit, RunGameResult, RunGameScreen>

/**
 * Runs the screens around a Tic Tac Toe game: prompts for player names, runs a
 * confirm quit screen, and offers a chance to play again. Delegates to [TakeTurnsWorkflow]
 * for the actual playing of the game.
 */
class RealRunGameWorkflow(
  private val takeTurnsWorkflow: TakeTurnsWorkflow,
  private val gameLog: GameLog
) : RunGameWorkflow,
    StatefulWorkflow<Unit, RunGameState, RunGameResult,
        RunGameScreen>() {

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): RunGameState = snapshot?.let { RunGameState.fromSnapshot(snapshot.bytes) }
      ?: NewGame()

  override fun compose(
    input: Unit,
    state: RunGameState,
    context: WorkflowContext<RunGameState, RunGameResult>
  ): RunGameScreen {
    return when (state) {
      is NewGame -> {
        val emptyGameScreen = GamePlayScreen()

        val eventHandler = context.onEvent<NewGameScreen.Event> { event ->
          when (event) {
            CancelNewGame -> emitOutput(CanceledStart)
            is StartGame -> enterState(Playing(PlayerInfo(event.x, event.o)))
          }
        }

        subflowScreen(
            base = emptyGameScreen,
            subflow = NewGameScreen(state.defaultXName, state.defaultOName, eventHandler)
        )
      }

      is Playing -> {
        // context.composeChild starts takeTurnsWorkflow, or keeps it running if it was
        // already going. TakeTurnsWorkflow.compose is immediately called,
        // and the GamePlayScreen it renders is immediately returned.
        val takeTurnsScreen = context.composeChild(takeTurnsWorkflow, state.playerInfo) { output ->
          when (output.ending) {
            Quitted -> enterState(MaybeQuitting(output, state.playerInfo))
            else -> enterState(GameOver(state.playerInfo, output))
          }
        }

        simpleScreen(takeTurnsScreen)
      }

      is MaybeQuitting -> {
        alertScreen(
            base = GamePlayScreen(state.playerInfo, state.completedGame.lastTurn),
            alert = maybeQuitScreen(
                confirmQuit = context.onEvent {
                  enterState(MaybeQuittingForSure(state.playerInfo, state.completedGame))
                },
                continuePlaying = context.onEvent {
                  enterState(Playing(state.playerInfo, state.completedGame.lastTurn))
                }
            )
        )
      }

      is MaybeQuittingForSure -> {
        nestedAlertsScreen(
            GamePlayScreen(state.playerInfo, state.completedGame.lastTurn),
            maybeQuitScreen(),
            maybeQuitScreen(
                message = "Really?",
                positive = "Yes!!",
                negative = "Sigh, no",
                confirmQuit = context.onEvent {
                  enterState(GameOver(state.playerInfo, state.completedGame))
                },
                continuePlaying = context.onEvent {
                  enterState(Playing(state.playerInfo, state.completedGame.lastTurn))
                }
            )
        )
      }

      is GameOver -> {
        if (state.syncState == SAVING) {
          context.onSuccess(gameLog.logGame(state.completedGame)) { result ->
            when (result) {
              TRY_LATER -> enterState(state.copy(syncState = SAVE_FAILED))
              LOGGED -> enterState(state.copy(syncState = SAVED))
            }
          }
        }

        GameOverScreen(
            state,
            context.onEvent { event ->
              when (event) {
                TrySaveAgain -> {
                  check(state.syncState == SAVE_FAILED) {
                    "Should only receive $TrySaveAgain in syncState $SAVE_FAILED, " +
                        "was ${state.syncState}"
                  }
                  enterState(state.copy(syncState = SAVING))
                }
                PlayAgain -> {
                  val (x, o) = state.playerInfo
                  enterState(NewGame(x, o))
                }
                Exit -> emitOutput(FinishedPlaying)
              }
            }
        ).let(::simpleScreen)
      }
    }
  }

  override fun snapshotState(state: RunGameState): Snapshot = state.toSnapshot()

  private fun nestedAlertsScreen(
    base: Any,
    vararg alerts: AlertScreen
  ): RunGameScreen {
    return AlertContainerScreen(
        PanelContainerScreen<Any, Any>(base), *alerts
    )
  }

  private fun alertScreen(
    base: Any,
    alert: AlertScreen
  ): RunGameScreen {
    return AlertContainerScreen(
        PanelContainerScreen<Any, Any>(base), alert
    )
  }

  private fun subflowScreen(
    base: Any,
    subflow: Any
  ): RunGameScreen {
    return AlertContainerScreen(subflow.asPanelOver(base))
  }

  private fun simpleScreen(screen: Any): RunGameScreen {
    return AlertContainerScreen(PanelContainerScreen<Any, Any>(screen))
  }

  private fun maybeQuitScreen(
    message: String = "Do you really want to concede the game?",
    positive: String = "I Quit",
    negative: String = "No",
    confirmQuit: EventHandler<Unit> = EventHandler { },
    continuePlaying: EventHandler<Unit> = EventHandler { }
  ): AlertScreen {
    return AlertScreen(
        buttons = mapOf(
            POSITIVE to positive,
            NEGATIVE to negative
        ),
        message = message,
        onEvent = { alertEvent ->
          when (alertEvent) {
            is ButtonClicked -> when (alertEvent.button) {
              POSITIVE -> confirmQuit()
              NEGATIVE -> continuePlaying()
              NEUTRAL -> throw IllegalArgumentException()
            }
            Canceled -> continuePlaying()
          }
        }
    )
  }
}
