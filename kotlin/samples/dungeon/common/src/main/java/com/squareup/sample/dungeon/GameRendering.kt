package com.squareup.sample.dungeon

data class GameRendering(
  val board: Board,
  val onEvent: (GameEvent) -> Unit
)
