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
import com.squareup.workflow.ui.AlertContainerScreen
import com.squareup.workflow.ui.BackStackScreen

class DungeonAppWorkflow(
  private val gameSessionWorkflow: GameSessionWorkflow,
  private val boardLoader: BoardLoader
) : StatefulWorkflow<Props, State, Nothing, AlertContainerScreen<BackStackScreen<Any>>>() {

  data class Props(val paused: Boolean = false)

  sealed class State {
    object LoadingBoardList : State()
    data class ChoosingBoard(val boards: List<Pair<String, Board>>) : State()
    data class PlayingGame(
      val previousState: ChoosingBoard,
      val boardPath: BoardPath
    ) : State()
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
  ): AlertContainerScreen<BackStackScreen<Any>> {
    fun createListScreen(state: ChoosingBoard) = DisplayBoardsListScreen(
        boards = state.boards.map { it.second },
        onBoardSelected = { index -> context.actionSink.send(selectBoard(index)) }
    )

    return when (state) {
      LoadingBoardList -> {
        context.runningWorker(boardLoader.loadAvailableBoards()) { displayBoards(it) }
        AlertContainerScreen(BackStackScreen(state))
      }

      is ChoosingBoard -> {
        val listScreen = createListScreen(state)
        AlertContainerScreen(BackStackScreen(listScreen))
      }

      is PlayingGame -> {
        val listScreen = createListScreen(state.previousState)
        val sessionProps = GameSessionWorkflow.Props(state.boardPath, props.paused)
        val gameScreen = context.renderChild(gameSessionWorkflow, sessionProps) {
          displayBoards(state.previousState.boards.toMap())
        }
        AlertContainerScreen(BackStackScreen(listScreen, gameScreen.baseScreen), gameScreen.modals)
      }
    }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private fun displayBoards(boards: Map<String, Board>) = action {
    nextState = ChoosingBoard(boards.toList())
  }

  private fun selectBoard(index: Int) = action {
    // No-op if we're not in the ChoosingBoard state.
    (nextState as? ChoosingBoard)?.let { choosingBoard ->
      nextState = PlayingGame(choosingBoard, choosingBoard.boards[index].first)
    }
  }
}
