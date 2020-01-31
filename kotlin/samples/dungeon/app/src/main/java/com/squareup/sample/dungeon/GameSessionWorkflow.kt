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
package com.squareup.sample.dungeon

import android.os.Vibrator
import com.squareup.sample.dungeon.GameSessionWorkflow.Props
import com.squareup.sample.dungeon.GameSessionWorkflow.State
import com.squareup.sample.dungeon.GameSessionWorkflow.State.GameOver
import com.squareup.sample.dungeon.GameSessionWorkflow.State.Loading
import com.squareup.sample.dungeon.GameSessionWorkflow.State.Running
import com.squareup.sample.dungeon.GameWorkflow.Output.PlayerWasEaten
import com.squareup.sample.dungeon.GameWorkflow.Output.Vibrate
import com.squareup.sample.dungeon.board.Board
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.action
import com.squareup.workflow.ui.modal.AlertContainerScreen
import com.squareup.workflow.ui.modal.AlertScreen
import com.squareup.workflow.ui.modal.AlertScreen.Button.POSITIVE

typealias BoardPath = String

/**
 * Workflow that loads a game board, runs the game, and displays game over screens.
 */
class GameSessionWorkflow(
  private val gameWorkflow: GameWorkflow,
  private val vibrator: Vibrator,
  private val boardLoader: BoardLoader
) : StatefulWorkflow<Props, State, Nothing, AlertContainerScreen<Any>>() {

  data class Props(
    val boardPath: BoardPath,
    val paused: Boolean = false
  )

  sealed class State {
    object Loading : State()
    data class Running(val board: Board) : State()
    data class GameOver(val board: Board) : State()
  }

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = Loading

  override fun render(
    props: Props,
    state: State,
    context: RenderContext<State, Nothing>
  ): AlertContainerScreen<Any> = when (state) {

    Loading -> {
      context.runningWorker(boardLoader.loadBoard(props.boardPath)) { StartRunning(it) }
      AlertContainerScreen(Loading)
    }

    is Running -> {
      val gameInput = GameWorkflow.Props(state.board, paused = props.paused)
      val gameScreen = context.renderChild(gameWorkflow, gameInput) {
        handleGameOutput(it, state.board)
      }
      AlertContainerScreen(gameScreen)
    }

    is GameOver -> {
      val gameInput = GameWorkflow.Props(state.board)
      val gameScreen = context.renderChild(gameWorkflow, gameInput) { noAction() }

      val gameOverDialog = AlertScreen(
          buttons = mapOf(POSITIVE to "Restart"),
          message = "You've been eaten, try again.",
          cancelable = false,
          onEvent = { context.actionSink.send(restartGame()) }
      )

      AlertContainerScreen(gameScreen, gameOverDialog)
    }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private class StartRunning(val board: Board) : WorkflowAction<State, Nothing> {
    override fun Updater<State, Nothing>.apply() {
      nextState = Running(board)
    }
  }

  private fun handleGameOutput(
    output: GameWorkflow.Output,
    board: Board
  ) = action("handleGameOutput") {
    when (output) {
      Vibrate -> vibrate(50)
      PlayerWasEaten -> {
        nextState = GameOver(board)
        vibrate(20)
        vibrate(20)
        vibrate(20)
        vibrate(20)
        vibrate(1000)
      }
    }
  }

  private fun restartGame() = action("restartGame") { nextState = Loading }

  private fun vibrate(durationMs: Long) {
    @Suppress("DEPRECATION")
    vibrator.vibrate(durationMs)
  }
}
