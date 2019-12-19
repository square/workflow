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
import com.squareup.workflow.WorkflowAction.Updater

typealias TakeTurnsWorkflow = Workflow<TakeTurnsProps, CompletedGame, GamePlayScreen>

class TakeTurnsProps private constructor(
  val playerInfo: PlayerInfo,
  val initialTurn: Turn = Turn()
) {
  companion object {
    fun newGame(playerInfo: PlayerInfo): TakeTurnsProps = TakeTurnsProps(playerInfo)
    fun resumeGame(
      playerInfo: PlayerInfo,
      turn: Turn
    ): TakeTurnsProps = TakeTurnsProps(playerInfo, turn)
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
    StatefulWorkflow<TakeTurnsProps, Turn, CompletedGame, GamePlayScreen>() {

  sealed class Action : WorkflowAction<Turn, CompletedGame> {
    class TakeSquare(
      private val row: Int,
      private val col: Int
    ) : Action() {
      override fun Updater<Turn, CompletedGame>.apply() {
        val newBoard = nextState.board.takeSquare(row, col, nextState.playing)

        when {
          newBoard.hasVictory() ->
            setOutput(CompletedGame(Victory, nextState.copy(board = newBoard)))

          newBoard.isFull() -> setOutput(CompletedGame(Draw, nextState.copy(board = newBoard)))

          else -> nextState = Turn(playing = nextState.playing.other, board = newBoard)
        }
      }
    }

    object Quit : Action() {
      override fun Updater<Turn, CompletedGame>.apply() {
        setOutput(CompletedGame(Quitted, nextState))
      }
    }
  }

  override fun initialState(
    props: TakeTurnsProps,
    snapshot: Snapshot?
  ): Turn = props.initialTurn

  override fun render(
    props: TakeTurnsProps,
    state: Turn,
    context: RenderContext<Turn, CompletedGame>
  ): GamePlayScreen = GamePlayScreen(
      playerInfo = props.playerInfo,
      gameState = state,
      onQuit = { context.actionSink.send(Quit) },
      onClick = { row, col -> context.actionSink.send(TakeSquare(row, col)) }
  )

  override fun snapshotState(state: Turn) = Snapshot.EMPTY
}
