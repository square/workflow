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

import com.squareup.sample.tictactoe.Ending.Draw
import com.squareup.sample.tictactoe.Ending.Quitted
import com.squareup.sample.tictactoe.Ending.Victory
import com.squareup.sample.tictactoe.TakeTurnsEvent.Quit
import com.squareup.sample.tictactoe.TakeTurnsEvent.TakeSquare
import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.rx2.Reactor
import com.squareup.workflow.rx2.EventChannel
import com.squareup.workflow.rx2.doLaunch
import io.reactivex.Single

/**
 * Models the turns of a Tic Tac Toe game, alternating between [Player.X]
 * and [Player.O]. Finishes with a [report][CompletedGame] of the last turn of the game,
 * and an [Ending] condition of [Victory], [Draw] or [Quitted].
 *
 * To resume a quit game, [launch] with [CompletedGame.lastTurn].
 *
 * http://go/sf-taketurns
 */
class TakeTurnsReactor : Reactor<Turn, TakeTurnsEvent, CompletedGame> {
  override fun launch(
    initialState: Turn,
    workflows: WorkflowPool
  ): Workflow<Turn, TakeTurnsEvent, CompletedGame> = doLaunch(initialState, workflows)

  override fun onReact(
    state: Turn,
    events: EventChannel<TakeTurnsEvent>,
    workflows: WorkflowPool
  ): Single<out Reaction<Turn, CompletedGame>> = events.select {
    onEvent<TakeSquare> { takeSquareAndReact(state, it.row, it.col) }

    onEvent<Quit> { FinishWith(CompletedGame(Quitted, state)) }
  }

  private fun takeSquareAndReact(
    state: Turn,
    row: Int,
    col: Int
  ): Reaction<Turn, CompletedGame> {
    checkIndex(row)
    checkIndex(col)

    with(state) {
      if (board[row][col] != null) return EnterState(state)

      val newRow: List<Player?> = board[row].toMutableList()
          .apply { this[col] = playing }
      val newBoard: Board = board.toMutableList()
          .apply { this[row] = newRow }

      checkForEnded(newBoard)?.let {
        return FinishWith(
            CompletedGame(it, state.copy(board = newBoard))
        )
      }

      return EnterState(this.copy(playing = playing.other, board = newBoard))
    }
  }

  private fun checkIndex(index: Int) {
    check(index in 0..2) { "Expected $index to be in 0..2" }
  }

  private fun checkForEnded(board: Board): Ending? {
    if (board.hasVictory()) return Victory
    if (board.isFull()) return Draw
    return null
  }
}
