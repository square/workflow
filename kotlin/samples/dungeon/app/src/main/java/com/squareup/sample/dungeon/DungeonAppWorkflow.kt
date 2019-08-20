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
import com.squareup.workflow.runningWorker

private typealias BoardPath = String

class DungeonAppWorkflow(
  private val gameWorkflow: GameWorkflow,
  private val vibrator: Vibrator,
  private val boardLoader: BoardLoader
) : StatefulWorkflow<BoardPath, State, Nothing, Any>() {

  sealed class State {
    object Loading : State()
    data class Running(val board: Board) : State()
  }

  private fun startRunning(board: Board) = WorkflowAction<State, Nothing> {
    state = Running(board)
    null
  }

  override fun initialState(
    props: BoardPath,
    snapshot: Snapshot?
  ): State = Loading

  override fun render(
    props: BoardPath,
    state: State,
    context: RenderContext<State, Nothing>
  ): Any {
    return when (state) {
      Loading -> {
        context.runningWorker(boardLoader.load(props)) { startRunning(it) }
        Loading
      }

      is Running -> {
        val gameInput = GameWorkflow.Props(state.board)
        context.renderChild(gameWorkflow, gameInput) { output ->
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
          noAction()
        }
      }
    }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private fun vibrate(durationMs: Long) {
    @Suppress("DEPRECATION")
    vibrator.vibrate(durationMs)
  }
}
