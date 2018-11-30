package com.squareup.sample.tictactoe

import com.squareup.sample.tictactoe.Player.O
import com.squareup.sample.tictactoe.Player.X
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
  return asSequence().flatten().firstOrNull { it != null }
      ?.let { false }
      ?: true
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
