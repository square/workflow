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
package com.squareup.sample.tictactoe.android

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.sample.tictactoe.CompletedGame
import com.squareup.sample.tictactoe.Ending.Draw
import com.squareup.sample.tictactoe.Ending.Quitted
import com.squareup.sample.tictactoe.Ending.Victory
import com.squareup.sample.tictactoe.GameOverScreen
import com.squareup.sample.tictactoe.RunGameEvent.NoMore
import com.squareup.sample.tictactoe.RunGameEvent.PlayAgain
import com.squareup.sample.tictactoe.RunGameEvent.TrySaveAgain
import com.squareup.sample.tictactoe.SyncState.SAVED
import com.squareup.sample.tictactoe.SyncState.SAVE_FAILED
import com.squareup.sample.tictactoe.SyncState.SAVING
import com.squareup.sample.tictactoe.android.GamePlayCoordinator.Companion.renderBoard
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
  private lateinit var banner: TextView
  private lateinit var button1: Button
  private lateinit var button2: Button

  override fun attach(view: View) {
    super.attach(view)

    board = view.findViewById(R.id.board_view)
    banner = view.findViewById(R.id.game_banner)
    button1 = view.findViewById(R.id.game_banner_button_1)
    button2 = view.findViewById(R.id.game_banner_button_2)

    button2.visibility = View.VISIBLE
    button2.text = view.resources.getText(R.string.exit)

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
    button2.setOnClickListener { screen.onEvent(NoMore) }
    // Start a new game when they hit the back button.
    view.setBackHandler { screen.onEvent(PlayAgain) }

    when (screen.data.syncState) {
      SAVING -> {
        button1.isEnabled = false
        button1.text = "…saving…"
        button1.setOnClickListener(null)
      }
      SAVE_FAILED -> {
        button1.isEnabled = true
        button1.text = "Save Failed Try Again"
        button1.setOnClickListener { screen.onEvent(TrySaveAgain) }
      }
      SAVED -> {
        button1.visibility = View.GONE
        button1.setOnClickListener(null)
      }
    }

    renderGame(board, banner, screen.data.completedGame)
  }

  private fun renderGame(
    board: ViewGroup,
    banner: TextView,
    completedGame: CompletedGame
  ) {
    renderResult(banner, completedGame)
    renderBoard(board, completedGame.lastTurn.board)
  }

  private fun renderResult(
    textView: TextView,
    completedGame: CompletedGame
  ) {
    val player = completedGame.lastTurn.playing
    val playerName = completedGame.lastTurn.players[player]

    val message: String = when (completedGame.ending) {
      Victory -> "The $player's have it, $playerName wins!"
      Draw -> "It's a draw."
      Quitted -> "$playerName ($player) is a quitter!"
    }

    textView.text = message
  }

  /** Note how easily  we're sharing this layout with [GamePlayCoordinator]. */
  companion object : ViewBinding<GameOverScreen> by LayoutBinding.of(
      R.layout.game_play_layout, ::GameOverCoordinator
  )
}
