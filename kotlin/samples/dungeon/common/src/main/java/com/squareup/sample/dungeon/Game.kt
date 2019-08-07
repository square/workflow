package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.Board.Location

data class Game(
  val board: Board,
  val player: Player,
  val playerLocation: Location
) {

  fun withPlayer(): Board = board.withOverlay(mapOf(playerLocation to player.cell))

  override fun toString(): String = withPlayer().toString()
}
