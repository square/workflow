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

import com.squareup.sample.container.panel.PanelContainerScreen
import com.squareup.sample.container.panel.firstInPanelOver
import com.squareup.sample.gameworkflow.Action.CancelNewGame
import com.squareup.sample.gameworkflow.Action.ConfirmQuit
import com.squareup.sample.gameworkflow.Action.ConfirmQuitAgain
import com.squareup.sample.gameworkflow.Action.ContinuePlaying
import com.squareup.sample.gameworkflow.Action.Exit
import com.squareup.sample.gameworkflow.Action.HandleLogGame
import com.squareup.sample.gameworkflow.Action.PlayAgain
import com.squareup.sample.gameworkflow.Action.StartGame
import com.squareup.sample.gameworkflow.Action.StopPlaying
import com.squareup.sample.gameworkflow.Action.TrySaveAgain
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.GameLog.LogResult.LOGGED
import com.squareup.sample.gameworkflow.GameLog.LogResult.TRY_LATER
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
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.rx2.asWorker
import com.squareup.workflow.ui.modal.AlertContainerScreen
import com.squareup.workflow.ui.modal.AlertScreen
import com.squareup.workflow.ui.modal.AlertScreen.Button.NEGATIVE
import com.squareup.workflow.ui.modal.AlertScreen.Button.NEUTRAL
import com.squareup.workflow.ui.modal.AlertScreen.Button.POSITIVE
import com.squareup.workflow.ui.modal.AlertScreen.Event.ButtonClicked
import com.squareup.workflow.ui.modal.AlertScreen.Event.Canceled

enum class RunGameResult {
  CanceledStart,
  FinishedPlaying
}

typealias RunGameScreen = AlertContainerScreen<PanelContainerScreen<Any, Any>>

/**
 * We define this otherwise redundant typealias to keep composite workflows
 * that build on [RunGameWorkflow] decoupled from it, for ease of testing.
 */
typealias RunGameWorkflow = Workflow<Unit, RunGameResult, RunGameScreen>

sealed class Action : WorkflowAction<RunGameState, RunGameResult> {
  object CancelNewGame : Action()

  class StartGame(
    val x: String,
    val o: String
  ) : Action()

  class StopPlaying(
    val game: CompletedGame
  ) : Action()

  object ConfirmQuit : Action()

  class ContinuePlaying(
    val playerInfo: PlayerInfo,
    val turn: Turn
  ) : Action()

  object ConfirmQuitAgain : Action()

  class HandleLogGame(val result: GameLog.LogResult) : Action()

  object TrySaveAgain : Action()
  object PlayAgain : Action()
  object Exit : Action()

  // You'll notice a lot of lines like `val oldState = state as...`. Those are a
  // signal that this workflow is too big, and should be refactored into something
  // like one workflow per screen.

  override fun Updater<RunGameState, RunGameResult>.apply() {

    when (this@Action) {
      CancelNewGame -> setOutput(CanceledStart)

      is StartGame -> nextState = Playing(PlayerInfo(x, o))

      is StopPlaying -> {
        val oldState = nextState as Playing
        nextState = when (game.ending) {
          Quitted -> MaybeQuitting(oldState.playerInfo, game)
          else -> GameOver(oldState.playerInfo, game)
        }
      }

      ConfirmQuit -> {
        val oldState = nextState as MaybeQuitting
        nextState = MaybeQuittingForSure(oldState.playerInfo, oldState.completedGame)
      }

      is ContinuePlaying -> {
        nextState = Playing(playerInfo, turn)
      }

      ConfirmQuitAgain -> {
        val oldState = nextState as MaybeQuittingForSure
        nextState = GameOver(oldState.playerInfo, oldState.completedGame)
      }

      is HandleLogGame -> {
        val oldState = nextState as GameOver
        nextState = when (result) {
          TRY_LATER -> oldState.copy(syncState = SAVE_FAILED)
          LOGGED -> oldState.copy(syncState = SAVED)
        }
      }

      TrySaveAgain -> {
        val oldState = nextState as GameOver
        check(oldState.syncState == SAVE_FAILED) {
          "Should only receive $TrySaveAgain in syncState $SAVE_FAILED, " +
              "was ${oldState.syncState}"
        }
        nextState = oldState.copy(syncState = SAVING)
      }

      PlayAgain -> {
        val (x, o) = (nextState as GameOver).playerInfo
        nextState = NewGame(x, o)
      }

      Exit -> setOutput(FinishedPlaying)
    }
  }
}

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
    props: Unit,
    snapshot: Snapshot?
  ): RunGameState {
    return snapshot?.let { RunGameState.fromSnapshot(snapshot.bytes) }
        ?: NewGame()
  }

  override fun render(
    props: Unit,
    state: RunGameState,
    context: RenderContext<RunGameState, RunGameResult>
  ): RunGameScreen = when (state) {
    is NewGame -> {
      val emptyGameScreen = GamePlayScreen()

      subflowScreen(
          base = emptyGameScreen,
          subflow = NewGameScreen(
              state.defaultXName,
              state.defaultOName,
              onCancel = { context.actionSink.send(CancelNewGame) },
              onStartGame = { x, o -> context.actionSink.send(StartGame(x, o)) }
          )
      )
    }

    is Playing -> {
      // context.renderChild starts takeTurnsWorkflow, or keeps it running if it was
      // already going. TakeTurnsWorkflow.render is immediately called,
      // and the GamePlayScreen it renders is immediately returned.
      val takeTurnsScreen = context.renderChild(
          takeTurnsWorkflow,
          props = state.resume
              ?.let { TakeTurnsProps.resumeGame(state.playerInfo, it) }
              ?: TakeTurnsProps.newGame(state.playerInfo)
      ) { StopPlaying(it) }

      simpleScreen(takeTurnsScreen)
    }

    is MaybeQuitting -> {
      alertScreen(
          base = GamePlayScreen(state.playerInfo, state.completedGame.lastTurn),
          alert = maybeQuitScreen(
              confirmQuit = { context.actionSink.send(ConfirmQuit) },
              continuePlaying = {
                context.actionSink.send(ContinuePlaying(state.playerInfo, state.completedGame.lastTurn))
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
              confirmQuit = { context.actionSink.send(ConfirmQuitAgain) },
              continuePlaying = {
                context.actionSink.send(ContinuePlaying(state.playerInfo, state.completedGame.lastTurn))
              }
          )
      )
    }

    is GameOver -> {
      if (state.syncState == SAVING) {
        context.runningWorker(gameLog.logGame(state.completedGame).asWorker()) {
          HandleLogGame(it)
        }
      }

      GameOverScreen(
          state,
          onTrySaveAgain = { context.actionSink.send(TrySaveAgain) },
          onPlayAgain = { context.actionSink.send(PlayAgain) },
          onExit = { context.actionSink.send(Exit) }
      ).let(::simpleScreen)
    }
  }

  override fun snapshotState(state: RunGameState): Snapshot = state.toSnapshot()

  private fun nestedAlertsScreen(
    base: Any,
    vararg alerts: AlertScreen
  ): RunGameScreen {
    return AlertContainerScreen(
        PanelContainerScreen(base), *alerts
    )
  }

  private fun alertScreen(
    base: Any,
    alert: AlertScreen
  ): RunGameScreen {
    return AlertContainerScreen(
        PanelContainerScreen(base), alert
    )
  }

  private fun subflowScreen(
    base: Any,
    subflow: Any
  ): RunGameScreen {
    return AlertContainerScreen(subflow.firstInPanelOver(base))
  }

  private fun simpleScreen(screen: Any): RunGameScreen {
    return AlertContainerScreen(PanelContainerScreen(screen))
  }

  private fun maybeQuitScreen(
    message: String = "Do you really want to concede the game?",
    positive: String = "I Quit",
    negative: String = "No",
    confirmQuit: () -> Unit = { },
    continuePlaying: () -> Unit = { }
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
