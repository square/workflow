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
package com.squareup.sample.gameworkflow

/**
 * The rows and columns of a tic tac toe board. Each cell
 * holds either a [Player] symbol, or null if it is unoccupied.
 */
typealias Board = List<List<Player?>>

fun Board.isFull(): Boolean {
  asSequence().flatten()
      .forEach { if (it == null) return false }
  return true
}

fun Board.hasVictory(): Boolean {
  var done = false

  // Across
  var row = 0
  while (!done && row < 3) {
    done =
      this[row][0] != null &&
          this[row][0] === this[row][1] &&
          this[row][0] === this[row][2]
    row++
  }

  // Down
  var col = 0
  while (!done && col < 3) {
    done =
      this[0][col] != null &&
          this[0][col] === this[1][col] &&
          this[0][col] === this[2][col]
    col++
  }

  // Diagonal
  done = done or (this[0][0] != null &&
      this[0][0] === this[1][1] &&
      this[0][0] === this[2][2])

  done = done or (this[0][2] != null &&
      this[0][2] === this[1][1] &&
      this[0][2] === this[2][0])

  return done
}

fun Board.takeSquare(
  row: Int,
  col: Int,
  player: Player
): Board {
  checkIndex(row)
  checkIndex(col)
  if (this[row][col] != null) return this

  val newRow: List<Player?> = this[row].toMutableList()
      .apply { this[col] = player }

  return toMutableList()
      .apply { this[row] = newRow }
}

private fun checkIndex(index: Int) {
  check(index in 0..2) { "Expected $index to be in 0..2" }
}
