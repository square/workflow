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

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.squareup.sample.gameworkflow.Ending.Draw
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.SyncState.SAVED
import com.squareup.sample.gameworkflow.SyncState.SAVE_FAILED
import com.squareup.sample.gameworkflow.SyncState.SAVING
import com.squareup.sample.tictactoe.databinding.BoardBinding
import com.squareup.sample.tictactoe.databinding.GamePlayLayoutBinding
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.backPressedHandler

internal class GameOverLayoutRunner(
  private val binding: GamePlayLayoutBinding
) : LayoutRunner<GameOverScreen> {

  private val saveItem: MenuItem = binding.gamePlayToolbar.menu.add("")
      .apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
      }

  private val exitItem: MenuItem = binding.gamePlayToolbar.menu.add("Exit")
      .apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
      }

  override fun showRendering(
    rendering: GameOverScreen,
    viewEnvironment: ViewEnvironment
  ) {
    exitItem.setOnMenuItemClickListener {
      rendering.onPlayAgain()
      true
    }
    binding.root.backPressedHandler = { rendering.onExit() }

    when (rendering.endGameState.syncState) {
      SAVING -> {
        saveItem.isEnabled = false
        saveItem.title = "saving…"
        saveItem.setOnMenuItemClickListener(null)
      }
      SAVE_FAILED -> {
        saveItem.isEnabled = true
        saveItem.title = "Unsaved"
        saveItem.setOnMenuItemClickListener {
          rendering.onTrySaveAgain()
          true
        }
      }
      SAVED -> {
        saveItem.isVisible = false
        saveItem.setOnMenuItemClickListener(null)
      }
    }

    renderGame(
        binding.gamePlayBoard, binding.gamePlayToolbar, rendering.endGameState.completedGame,
        rendering.endGameState.playerInfo
    )
  }

  private fun renderGame(
    boardView: BoardBinding,
    toolbar: Toolbar,
    completedGame: CompletedGame,
    playerInfo: PlayerInfo
  ) {
    renderResult(toolbar, completedGame, playerInfo)
    completedGame.lastTurn.board.render(boardView.root)
  }

  private fun renderResult(
    toolbar: Toolbar,
    completedGame: CompletedGame,
    playerInfo: PlayerInfo
  ) {
    val symbol = completedGame.lastTurn.playing.symbol
    val playerName = completedGame.lastTurn.playing.name(playerInfo)

    toolbar.title = if (playerName.isEmpty()) {
      when (completedGame.ending) {
        Victory -> "$symbol wins!"
        Draw -> "It's a draw."
        Quitted -> "$symbol is a quitter!"
      }
    } else {
      when (completedGame.ending) {
        Victory -> "The $symbol's have it, $playerName wins!"
        Draw -> "It's a draw."
        Quitted -> "$playerName ($symbol) is a quitter!"
      }
    }
  }

  /** Note how easily we're sharing this layout with [GamePlayViewFactory]. */
  companion object : ViewFactory<GameOverScreen> by bind(
      GamePlayLayoutBinding::inflate, ::GameOverLayoutRunner
  )
}
