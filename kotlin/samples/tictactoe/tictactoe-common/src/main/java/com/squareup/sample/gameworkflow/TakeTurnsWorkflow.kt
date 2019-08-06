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
import com.squareup.sample.gameworkflow.GamePlayScreen.Event.Quit
import com.squareup.sample.gameworkflow.GamePlayScreen.Event.TakeSquare
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noAction

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

  override fun initialState(
    input: TakeTurnsInput,
    snapshot: Snapshot?
  ): Turn = input.initialTurn

  override fun render(
    input: TakeTurnsInput,
    state: Turn,
    context: RenderContext<Turn, CompletedGame>
  ): GamePlayScreen {
    return GamePlayScreen(
        playerInfo = input.playerInfo,
        gameState = state,
        onEvent = context.onEvent { event ->
          when (event) {
            is TakeSquare -> onTakeSquare(state, event)
            Quit -> emitOutput(CompletedGame(Quitted, state))
          }
        }
    )
  }

  private fun onTakeSquare(
    lastTurn: Turn,
    event: TakeSquare
  ): WorkflowAction<Turn, CompletedGame> {

    val newBoard = lastTurn.board.takeSquare(event.row, event.col, lastTurn.playing)

    return when {
      newBoard == lastTurn.board -> noAction()
      newBoard.hasVictory() -> emitOutput(CompletedGame(Victory, lastTurn.copy(board = newBoard)))
      newBoard.isFull() -> emitOutput(CompletedGame(Draw, lastTurn.copy(board = newBoard)))
      else -> enterState(Turn(playing = lastTurn.playing.other, board = newBoard))
    }
  }

  override fun snapshotState(state: Turn) = Snapshot.EMPTY
}
