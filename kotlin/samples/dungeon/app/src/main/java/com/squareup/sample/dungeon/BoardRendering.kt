package com.squareup.sample.dungeon

data class BoardRendering(
  val width: Int,
  val height: Int,
  val cells: List<BoardCell>
) {
  init {
    require(cells.size == width * height) {
      "Contents must be $width×$height=${width * height}, but was ${cells.size}"
    }
  }
}
