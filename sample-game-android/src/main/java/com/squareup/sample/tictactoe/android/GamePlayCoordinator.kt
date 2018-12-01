package com.squareup.sample.tictactoe.android

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.possiblefuture.tictactoeandroid.R
import com.squareup.sample.tictactoe.Board
import com.squareup.sample.tictactoe.GamePlayScreen
import com.squareup.sample.tictactoe.TakeTurnsEvent
import com.squareup.sample.tictactoe.TakeTurnsEvent.Quit
import com.squareup.sample.tictactoe.TakeTurnsEvent.TakeSquare
import com.squareup.sample.tictactoe.Turn
import com.squareup.viewbuilder.LayoutViewBuilder
import com.squareup.viewbuilder.ViewBuilder
import com.squareup.viewbuilder.setBackHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class GamePlayCoordinator(
  private val screens: Observable<out GamePlayScreen>
) : Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var board: ViewGroup
  private lateinit var banner: TextView

  override fun attach(view: View) {
    super.attach(view)

    board = view.findViewById(R.id.board_view)
    banner = view.findViewById(R.id.game_banner)
    view.findViewById<View>(R.id.game_banner_button_1)
        .visibility = View.INVISIBLE
    view.findViewById<View>(R.id.game_banner_button_2)
        .visibility = View.INVISIBLE

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
    renderPlayer(banner, screen.data)
    renderBoard(board, screen.data.board)

    setCellClickListeners(board, screen.data, screen.onEvent)
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

  private fun renderPlayer(
    banner: TextView,
    turn: Turn
  ) {
    val yourTurn = turn.players[turn.playing]
    val mark = turn.playing.name
    val message = String.format("%s, place your %s", yourTurn, mark)
    banner.text = message
  }

  companion object : ViewBuilder<GamePlayScreen> by LayoutViewBuilder.of(
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
