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

import com.squareup.sample.tictactoe.Ending.Quitted
import com.squareup.sample.tictactoe.GameLog.LogResult.LOGGED
import com.squareup.sample.tictactoe.GameLog.LogResult.TRY_LATER
import com.squareup.sample.tictactoe.Player.O
import com.squareup.sample.tictactoe.Player.X
import com.squareup.sample.tictactoe.RunGameEvent.ConfirmQuit
import com.squareup.sample.tictactoe.RunGameEvent.ContinuePlaying
import com.squareup.sample.tictactoe.RunGameEvent.NoMore
import com.squareup.sample.tictactoe.RunGameEvent.PlayAgain
import com.squareup.sample.tictactoe.RunGameEvent.StartGame
import com.squareup.sample.tictactoe.RunGameEvent.TrySaveAgain
import com.squareup.sample.tictactoe.RunGameResult.CanceledStart
import com.squareup.sample.tictactoe.RunGameResult.FinishedPlaying
import com.squareup.sample.tictactoe.RunGameState.GameOver
import com.squareup.sample.tictactoe.RunGameState.MaybeQuitting
import com.squareup.sample.tictactoe.RunGameState.NewGame
import com.squareup.sample.tictactoe.RunGameState.Playing
import com.squareup.sample.tictactoe.SyncState.SAVED
import com.squareup.sample.tictactoe.SyncState.SAVE_FAILED
import com.squareup.sample.tictactoe.SyncState.SAVING
import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.register
import com.squareup.workflow.rx2.EventChannel
import com.squareup.workflow.rx2.Reactor
import com.squareup.workflow.rx2.doLaunch
import com.squareup.workflow.rx2.singleWorker
import io.reactivex.Single

enum class RunGameResult {
  CanceledStart,
  FinishedPlaying
}

/**
 * Runs the screens around a Tic Tac Toe game: prompts for player names, runs a
 * confirm quit screen, and offers a chance to play again. Delegates to [TakeTurnsReactor]
 * for the actual playing of the game.
 *
 * http://go/sf-rungame
 */
class RunGameReactor(
  private val takeTurnsReactor: TakeTurnsReactor,
  gameLog: GameLog
) : Reactor<RunGameState, RunGameEvent, RunGameResult> {

  private val logGameWorker = singleWorker(gameLog::logGame)

  override fun launch(
    initialState: RunGameState,
    workflows: WorkflowPool
  ): Workflow<RunGameState, RunGameEvent, RunGameResult> {
    workflows.register(takeTurnsReactor)
    return doLaunch(initialState, workflows)
  }

  override fun onReact(
    state: RunGameState,
    events: EventChannel<RunGameEvent>,
    workflows: WorkflowPool
  ): Single<out Reaction<RunGameState, RunGameResult>> = when (state) {

    is NewGame -> events.select {
      onEvent<NoMore> { FinishWith(CanceledStart) }
      onEvent<StartGame> { EnterState(Playing(Turn(it.x, it.o))) }
    }

    is Playing -> events.select {
      workflows.onNextDelegateReaction(state) {
        when (it) {
          is EnterState -> EnterState(state.copy(delegateState = it.state))
          is FinishWith -> when (it.result.ending) {
            Quitted -> EnterState(MaybeQuitting(it.result))
            else -> EnterState(GameOver(it.result))
          }
        }
      }
    }

    is MaybeQuitting -> events.select {
      onEvent<ConfirmQuit> {
        EnterState(
            GameOver(state.completedGame)
        )
      }

      onEvent<ContinuePlaying> {
        EnterState(Playing(state.completedGame.lastTurn))
      }
    }

    is GameOver -> {
      events.select {
        if (state.syncState == SAVING) {
          workflows.onWorkerResult(logGameWorker, state.completedGame) {
            when (it) {
              TRY_LATER -> EnterState(state.copy(syncState = SAVE_FAILED))
              LOGGED -> EnterState(state.copy(syncState = SAVED))
            }
          }
        } else if (state.syncState == SAVE_FAILED) {
          onEvent<TrySaveAgain> { EnterState(state.copy(syncState = SAVING)) }
        }

        onEvent<PlayAgain> {
          workflows.abandonWorker(logGameWorker)

          with(state.completedGame.lastTurn) {
            EnterState(NewGame(players[X]!!, players[O]!!))
          }
        }

        onEvent<NoMore> { FinishWith(FinishedPlaying) }
      }
    }
  }
}
