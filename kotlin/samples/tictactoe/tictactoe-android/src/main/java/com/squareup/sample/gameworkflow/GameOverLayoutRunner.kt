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

import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.squareup.sample.gameworkflow.Ending.Draw
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.GameOverScreen.Event.Exit
import com.squareup.sample.gameworkflow.GameOverScreen.Event.PlayAgain
import com.squareup.sample.gameworkflow.GameOverScreen.Event.TrySaveAgain
import com.squareup.sample.gameworkflow.SyncState.SAVED
import com.squareup.sample.gameworkflow.SyncState.SAVE_FAILED
import com.squareup.sample.gameworkflow.SyncState.SAVING
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.setBackHandler

@UseExperimental(ExperimentalWorkflowUi::class)
internal class GameOverLayoutRunner(private val view: View) : LayoutRunner<GameOverScreen> {
  private val boardView: ViewGroup = view.findViewById(R.id.game_play_board)
  private val toolbar: Toolbar = view.findViewById(R.id.game_play_toolbar)

  private val saveItem: MenuItem = toolbar.menu.add("")
      .apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
      }

  private val exitItem: MenuItem = toolbar.menu.add("Exit")
      .apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
      }

  override fun showRendering(rendering: GameOverScreen) {
    exitItem.setOnMenuItemClickListener {
      rendering.onEvent(PlayAgain)
      true
    }
    view.setBackHandler { rendering.onEvent(Exit) }

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
          rendering.onEvent(TrySaveAgain)
          true
        }
      }
      SAVED -> {
        saveItem.isVisible = false
        saveItem.setOnMenuItemClickListener(null)
      }
    }

    renderGame(
        boardView, toolbar, rendering.endGameState.completedGame, rendering.endGameState.playerInfo
    )
  }

  private fun renderGame(
    boardView: ViewGroup,
    toolbar: Toolbar,
    completedGame: CompletedGame,
    playerInfo: PlayerInfo
  ) {
    renderResult(toolbar, completedGame, playerInfo)
    completedGame.lastTurn.board.render(boardView)
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

  /** Note how easily  we're sharing this layout with [GamePlayLayoutRunner]. */
  companion object : ViewBinding<GameOverScreen> by bind(
      R.layout.game_play_layout, ::GameOverLayoutRunner
  )
}
