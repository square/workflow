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
import android.widget.TextView
import android.widget.Toolbar
import com.squareup.coordinators.Coordinator
import com.squareup.sample.tictactoe.Board
import com.squareup.sample.tictactoe.GamePlayScreen
import com.squareup.sample.tictactoe.TakeTurnsEvent
import com.squareup.sample.tictactoe.TakeTurnsEvent.Quit
import com.squareup.sample.tictactoe.TakeTurnsEvent.TakeSquare
import com.squareup.sample.tictactoe.Turn
import com.squareup.viewregistry.LayoutBinding
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.setBackHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class GamePlayCoordinator(
  private val screens: Observable<out GamePlayScreen>
) : Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var board: ViewGroup
  private lateinit var toolbar: Toolbar

  override fun attach(view: View) {
    super.attach(view)

    board = view.findViewById(R.id.board_view)
    toolbar = view.findViewById(R.id.toolbar)

    subs.add(screens.subscribe { update(view, it) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  private fun update(
    view: View,
    screen: GamePlayScreen
  ) {
    renderPlayer(screen.gameState)
    renderBoard(board, screen.gameState.board)

    setCellClickListeners(board, screen.gameState, screen.onEvent)
    view.setBackHandler { screen.onEvent(Quit) }
  }

  private fun setCellClickListeners(
    viewGroup: ViewGroup,
    turn: Turn,
    onEvent: (TakeTurnsEvent) -> Unit
  ) {
    for (i in 0..8) {
      val cell = viewGroup.getChildAt(i)

      val row = i / 3
      val col = i % 3
      val box = turn.board[row][col]

      val cellClickListener =
        if (box != null) null
        else View.OnClickListener { onEvent(TakeSquare(row, col)) }

      cell.setOnClickListener(cellClickListener)
    }
  }

  private fun renderPlayer(turn: Turn) {
    val yourTurn = turn.players[turn.playing]
    val mark = turn.playing.name
    val message = String.format("%s, place your %s", yourTurn, mark)
    toolbar.title = message
  }

  companion object : ViewBinding<GamePlayScreen> by LayoutBinding.of(
      R.layout.game_play_layout, ::GamePlayCoordinator
  ) {
    /**
     * Shared code for painting a 3 x 3 set of [TextView] cells with the values
     * of a [Board]. Look, no subclassing.
     */
    internal fun renderBoard(
      viewGroup: ViewGroup,
      board: Board
    ) {
      for (i in 0..8) {
        val row = i / 3
        val col = i % 3

        val cell = viewGroup.getChildAt(i) as TextView
        val box = board[row][col]
        cell.text = box?.name ?: ""
      }
    }
  }
}
