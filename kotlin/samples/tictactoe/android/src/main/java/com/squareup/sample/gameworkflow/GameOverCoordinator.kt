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
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import com.squareup.coordinators.Coordinator
import com.squareup.sample.tictactoe.R
import com.squareup.sample.gameworkflow.Ending.Draw
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.RunGameEvent.NoMore
import com.squareup.sample.gameworkflow.RunGameEvent.PlayAgain
import com.squareup.sample.gameworkflow.RunGameEvent.TrySaveAgain
import com.squareup.sample.gameworkflow.SyncState.SAVED
import com.squareup.sample.gameworkflow.SyncState.SAVE_FAILED
import com.squareup.sample.gameworkflow.SyncState.SAVING
import com.squareup.sample.gameworkflow.GamePlayCoordinator.Companion.renderBoard
import com.squareup.viewregistry.LayoutBinding
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.setBackHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class GameOverCoordinator(
  private val screens: Observable<out GameOverScreen>
) : Coordinator() {

  private val subs = CompositeDisposable()

  private lateinit var board: ViewGroup
  private lateinit var toolbar: Toolbar
  private lateinit var saveItem: MenuItem
  private lateinit var exitItem: MenuItem

  override fun attach(view: View) {
    super.attach(view)

    board = view.findViewById(R.id.board_view)
    toolbar = view.findViewById(R.id.toolbar)

    with(toolbar.menu) {
      saveItem = add("")
      exitItem = add(R.string.exit)
    }

    saveItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    exitItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

    subs.add(screens.subscribe { s -> update(view, s) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  private fun update(
    view: View,
    screen: GameOverScreen
  ) {
    exitItem.setOnMenuItemClickListener {
      screen.onEvent(NoMore)
      true
    }
    // Start a new game when they hit the back button.
    view.setBackHandler { screen.onEvent(PlayAgain) }

    when (screen.data.syncState) {
      SAVING -> {
        saveItem.isEnabled = false
        saveItem.title = "saving…"
        saveItem.setOnMenuItemClickListener(null)
      }
      SAVE_FAILED -> {
        saveItem.isEnabled = true
        saveItem.title = "Unsaved"
        saveItem.setOnMenuItemClickListener {
          screen.onEvent(TrySaveAgain)
          true
        }
      }
      SAVED -> {
        saveItem.isVisible = false
        saveItem.setOnMenuItemClickListener(null)
      }
    }

    renderGame(screen.data.completedGame)
  }

  private fun renderGame(completedGame: CompletedGame) {
    renderResult(completedGame)
    renderBoard(board, completedGame.lastTurn.board)
  }

  private fun renderResult(completedGame: CompletedGame) {
    val player = completedGame.lastTurn.playing
    val playerName = completedGame.lastTurn.players[player]

    val message: String = when (completedGame.ending) {
      Victory -> "The $player's have it, $playerName wins!"
      Draw -> "It's a draw."
      Quitted -> "$playerName ($player) is a quitter!"
    }

    toolbar.title = message
  }

  /** Note how easily  we're sharing this layout with [GamePlayCoordinator]. */
  companion object : ViewBinding<GameOverScreen> by LayoutBinding.of(
      R.layout.game_play_layout, ::GameOverCoordinator
  )
}
