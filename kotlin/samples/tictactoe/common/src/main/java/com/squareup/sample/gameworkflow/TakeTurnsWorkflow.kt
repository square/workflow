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

import com.squareup.sample.gameworkflow.Ending.Draw
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.RealTakeTurnsWorkflow.Action.Quit
import com.squareup.sample.gameworkflow.RealTakeTurnsWorkflow.Action.TakeSquare
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Mutator

typealias TakeTurnsWorkflow = Workflow<TakeTurnsInput, CompletedGame, GamePlayScreen>

class TakeTurnsInput private constructor(
  val playerInfo: PlayerInfo,
  val initialTurn: Turn = Turn()
) {
  companion object {
    fun newGame(playerInfo: PlayerInfo): TakeTurnsInput = TakeTurnsInput(playerInfo)
    fun resumeGame(
      playerInfo: PlayerInfo,
      turn: Turn
    ): TakeTurnsInput = TakeTurnsInput(playerInfo, turn)
  }
}

/**
 * Models the turns of a Tic Tac Toe game, alternating between [Player.X]
 * and [Player.O]. Finishes with a [report][CompletedGame] of the last turn of the game,
 * and an [Ending] condition of [Victory], [Draw] or [Quitted].
 *
 * http://go/sf-taketurns
 */
class RealTakeTurnsWorkflow : TakeTurnsWorkflow,
    StatefulWorkflow<TakeTurnsInput, Turn, CompletedGame, GamePlayScreen>() {

  sealed class Action : WorkflowAction<Turn, CompletedGame> {
    class TakeSquare(
      private val row: Int,
      private val col: Int
    ) : Action() {
      override fun Mutator<Turn>.apply(): CompletedGame? {
        val newBoard = state.board.takeSquare(row, col, state.playing)

        return when {
          newBoard.hasVictory() -> CompletedGame(Victory, state.copy(board = newBoard))
          newBoard.isFull() -> CompletedGame(Draw, state.copy(board = newBoard))
          else -> {
            state = Turn(playing = state.playing.other, board = newBoard)
            null
          }
        }
      }
    }

    object Quit : Action() {
      override fun Mutator<Turn>.apply() = CompletedGame(Quitted, state)
    }
  }

  override fun initialState(
    input: TakeTurnsInput,
    snapshot: Snapshot?
  ): Turn = input.initialTurn

  override fun render(
    input: TakeTurnsInput,
    state: Turn,
    context: RenderContext<Turn, CompletedGame>
  ): GamePlayScreen {
    val sink = context.makeActionSink<Action>()

    return GamePlayScreen(
        playerInfo = input.playerInfo,
        gameState = state,
        onQuit = { sink.send(Quit) },
        onClick = { row, col -> sink.send(TakeSquare(row, col)) }
    )
  }

  override fun snapshotState(state: Turn) = Snapshot.EMPTY
}
