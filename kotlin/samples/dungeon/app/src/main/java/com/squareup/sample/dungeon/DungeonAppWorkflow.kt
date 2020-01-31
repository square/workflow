/*
 * Copyright 2020 Square Inc.
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

import com.squareup.sample.dungeon.DungeonAppWorkflow.Props
import com.squareup.sample.dungeon.DungeonAppWorkflow.State
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.ChoosingBoard
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.LoadingBoardList
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.PlayingGame
import com.squareup.sample.dungeon.board.Board
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action
import com.squareup.workflow.renderChild
import com.squareup.workflow.ui.modal.AlertContainerScreen

class DungeonAppWorkflow(
  private val gameSessionWorkflow: GameSessionWorkflow,
  private val boardLoader: BoardLoader
) : StatefulWorkflow<Props, State, Nothing, AlertContainerScreen<Any>>() {

  data class Props(val paused: Boolean = false)

  sealed class State {
    object LoadingBoardList : State()
    data class ChoosingBoard(val boards: List<Pair<String, Board>>) : State()
    data class PlayingGame(val boardPath: BoardPath) : State()
  }

  data class DisplayBoardsListScreen(
    val boards: List<Board>,
    val onBoardSelected: (index: Int) -> Unit
  )

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = LoadingBoardList

  override fun render(
    props: Props,
    state: State,
    context: RenderContext<State, Nothing>
  ): AlertContainerScreen<Any> = when (state) {

    LoadingBoardList -> {
      context.runningWorker(boardLoader.loadAvailableBoards()) { displayBoards(it) }
      AlertContainerScreen(state)
    }

    is ChoosingBoard -> {
      val screen = DisplayBoardsListScreen(
          boards = state.boards.map { it.second },
          onBoardSelected = { index -> context.actionSink.send(selectBoard(index)) }
      )
      AlertContainerScreen(screen)
    }

    is PlayingGame -> {
      val sessionProps = GameSessionWorkflow.Props(state.boardPath, props.paused)
      val gameScreen = context.renderChild(gameSessionWorkflow, sessionProps)
      gameScreen
    }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private fun displayBoards(boards: Map<String, Board>) = action {
    nextState = ChoosingBoard(boards.toList())
  }

  private fun selectBoard(index: Int) = action {
    // No-op if we're not in the ChoosingBoard state.
    val boards = (nextState as? ChoosingBoard)?.boards ?: return@action
    nextState = PlayingGame(boards[index].first)
  }
}
