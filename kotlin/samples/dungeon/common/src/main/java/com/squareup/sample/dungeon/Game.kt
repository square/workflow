package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.Board.Location

private val PLACEHOLDER_CELL = BoardCell("?")

data class Game(
  val board: Board,
  val playerLocation: Location,
  val aiActors: List<Location>
) {

  val isPlayerEaten: Boolean get() = aiActors.any { it == playerLocation }

  override fun toString(): String = board.withOverlay(
      (aiActors + playerLocation)
          .map { it to PLACEHOLDER_CELL }
          .toMap()).toString()
}
