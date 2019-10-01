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
import com.squareup.sample.dungeon.DungeonAppWorkflow.State
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.GameOver
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.Loading
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.Running
import com.squareup.sample.dungeon.GameWorkflow.Output.PlayerWasEaten
import com.squareup.sample.dungeon.GameWorkflow.Output.Vibrate
import com.squareup.sample.dungeon.board.Board
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.ui.AlertContainerScreen
import com.squareup.workflow.ui.AlertScreen
import com.squareup.workflow.ui.AlertScreen.Button.POSITIVE
import com.squareup.workflow.workflowAction

private typealias BoardPath = String

class DungeonAppWorkflow(
  private val gameWorkflow: GameWorkflow,
  private val vibrator: Vibrator,
  private val boardLoader: BoardLoader
) : StatefulWorkflow<BoardPath, State, Nothing, AlertContainerScreen<Any>>() {

  sealed class State {
    object Loading : State()
    data class Running(val board: Board) : State()
    data class GameOver(val board: Board) : State()
  }

  override fun initialState(
    props: BoardPath,
    snapshot: Snapshot?
  ): State = Loading

  override fun render(
    props: BoardPath,
    state: State,
    context: RenderContext<State, Nothing>
  ): AlertContainerScreen<Any> {
    return when (state) {
      Loading -> {
        context.runningWorker(boardLoader.load(props)) { startRunning(it) }
        AlertContainerScreen(Loading)
      }

      is Running -> {
        val gameInput = GameWorkflow.Props(state.board)
        val gameScreen = context.renderChild(gameWorkflow, gameInput) { handleGameOutput(it) }
        AlertContainerScreen(gameScreen)
      }

      is GameOver -> {
        val gameInput = GameWorkflow.Props(state.board)
        val gameScreen = context.renderChild(gameWorkflow, gameInput) { noAction() }

        val sink = context.makeActionSink<WorkflowAction<State, Nothing>>()
        val gameOverDialog = AlertScreen(
            buttons = mapOf(POSITIVE to "Restart"),
            message = "You've been eaten, try again.",
            cancelable = false,
            onEvent = { sink.send(restartGame()) }
        )

        AlertContainerScreen(gameScreen, gameOverDialog)
      }
    }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private fun startRunning(board: Board) = workflowAction {
    state = Running(board)
    return@workflowAction null
  }

  private fun handleGameOutput(output: GameWorkflow.Output) = workflowAction {
    when (output) {
      Vibrate -> vibrate(50)
      PlayerWasEaten -> {
        vibrate(20)
        vibrate(20)
        vibrate(20)
        vibrate(20)
        vibrate(1000)
      }
    }
    return@workflowAction null
  }

  private fun restartGame() = workflowAction {
    state = Loading
    return@workflowAction null
  }

  private fun vibrate(durationMs: Long) {
    @Suppress("DEPRECATION")
    vibrator.vibrate(durationMs)
  }
}
