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

import android.view.View
import android.view.ViewGroup
import com.squareup.sample.tictactoe.databinding.GamePlayLayoutBinding
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.backPressedHandler

internal val GamePlayViewFactory: ViewFactory<GamePlayScreen> =
  LayoutRunner.bind(GamePlayLayoutBinding::inflate) { rendering, _ ->
    renderBanner(rendering.gameState, rendering.playerInfo)
    rendering.gameState.board.render(gamePlayBoard.root)

    setCellClickListeners(gamePlayBoard.root, rendering.gameState, rendering.onClick)
    root.backPressedHandler = rendering.onQuit
  }

private fun setCellClickListeners(
  viewGroup: ViewGroup,
  turn: Turn,
  takeSquareHandler: (row: Int, col: Int) -> Unit
) {
  for (i in 0..8) {
    val cell = viewGroup.getChildAt(i)

    val row = i / 3
    val col = i % 3
    val box = turn.board[row][col]

    val cellClickListener =
      if (box != null) null
      else View.OnClickListener { takeSquareHandler(row, col) }

    cell.setOnClickListener(cellClickListener)
  }
}

private fun GamePlayLayoutBinding.renderBanner(
  turn: Turn,
  playerInfo: PlayerInfo
) {
  val mark = turn.playing.symbol
  val playerName = turn.playing.name(playerInfo)
      .trim()

  gamePlayToolbar.title = when {
    playerName.isEmpty() -> "Place your $mark"
    else -> "$playerName, place your $mark"
  }
}
