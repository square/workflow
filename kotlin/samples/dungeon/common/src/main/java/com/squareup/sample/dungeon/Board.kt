/*
 * Copyright 2019 Square Inc.
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
package com.squareup.sample.dungeon

import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.streams.toList

private const val WALL = "🌳"
private const val SPACE = " "

data class Board(
  val width: Int,
  val height: Int,
  val cells: List<BoardCell>
) {
  data class Location(
    val x: Int,
    val y: Int
  ) {
    override fun toString(): String = "($x, $y)"

    fun distanceTo(other: Location): Int =
      hypot(other.x - x.toFloat(), other.y - y.toFloat()).roundToInt()
  }

  init {
    require(cells.size == width * height) {
      "Cells must be $width×$height=${width * height}, but was ${cells.size}"
    }
  }

  fun cellIndexOf(
    x: Int,
    y: Int
  ): Int = (y * width) + x

  operator fun get(
    x: Int,
    y: Int
  ): BoardCell = cells[cellIndexOf(x, y)]

  fun withOverlay(actors: Map<Location, BoardCell>): Board {
    val playersByIndex = actors.mapKeys { (location, _) -> cellIndexOf(location.x, location.y) }
    return copy(cells = cells.mapIndexed { index, cell ->
      playersByIndex[index] ?: cell
    })
  }

  override fun toString(): String {
    return cells.asSequence()
        .chunked(width)
        .joinToString(separator = "\n") {
          it.joinToString(separator = "")
        }
  }

  companion object {
    val EMPTY = ((WALL.repeat(16)) +
        (WALL + SPACE.repeat(14) + WALL).repeat(14) +
        (WALL.repeat(16)))
        .codePoints()
        .toList()
        .map(::BoardCell)
  }
}
