package com.squareup.sample.tictactoe

/**
 * Events accepted by [TakeTurnsReactor].
 */
sealed class TakeTurnsEvent {
  data class TakeSquare(
    val row: Int,
    val col: Int
  ) : TakeTurnsEvent()

  object Quit : TakeTurnsEvent()
}
