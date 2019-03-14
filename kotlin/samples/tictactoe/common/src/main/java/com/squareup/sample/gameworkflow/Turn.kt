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
package com.squareup.sample.gameworkflow

import com.squareup.sample.gameworkflow.Player.O
import com.squareup.sample.gameworkflow.Player.X
import com.squareup.workflow.Snapshot
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.writeUtf8WithLength
import okio.ByteString

/**
 * The state of a tic tac toe game. Serves as the state of [TakeTurnsReactor].
 *
 * @param players A map of the [X] and [O] symbols to their player names.
 * @param playing The symbol of the player whose turn it is.
 * @param board The rows and columns of the tic tac toe [Board]. Each cell
 * holds either a [Player] symbol, or null if it is unoccupied.
 */
data class Turn(
  val players: Map<Player, String> = emptyMap(),
  val playing: Player = X,
  val board: Board = EMPTY_BOARD
) {
  constructor(
    x: String,
    o: String
  ) : this(
      players = mapOf(
          X to x,
          O to o
      )
  )

  fun toSnapshot(): Snapshot {
    return Snapshot.write { sink ->
      sink.writeUtf8WithLength(players[X]!!)
      sink.writeUtf8WithLength(players[O]!!)
    }
  }

  companion object {
    val EMPTY_BOARD = listOf(
        listOf(null, null, null),
        listOf(null, null, null),
        listOf(null, null, null)
    )

    fun fromSnapshot(byteString: ByteString): Turn {
      return byteString.parse { source ->
        Turn(
            x = source.readUtf8WithLength(),
            o = source.readUtf8WithLength()
        )
      }
    }
  }
}

enum class Player {
  X,
  O
}

val Player.other: Player
  get() = when (this) {
    X -> O
    O -> X
  }

typealias Board = List<List<Player?>>

internal fun Board.isFull(): Boolean {
  asSequence().flatten().forEach { if (it == null) return false }
  return true
}

internal fun Board.hasVictory(): Boolean {
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
