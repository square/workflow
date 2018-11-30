package com.squareup.sample.tictactoe

import com.squareup.workflow.Snapshot
import com.squareup.workflow.parse
import com.squareup.workflow.readByteStringWithLength
import com.squareup.workflow.writeByteStringWithLength
import okio.ByteString

/**
 * The [lastTurn] of a tic tac toe game, and its [ending]. Serves as the
 * result type for [TakeTurnsReactor].
 */
data class CompletedGame(
  val ending: Ending,
  val lastTurn: Turn
) {

  fun toSnapshot(): Snapshot {
    return Snapshot.write { sink ->
      sink.writeInt(this.ending.ordinal)
      sink.writeByteStringWithLength(this.lastTurn.toSnapshot().bytes)
    }
  }

  companion object {
    fun fromSnapshot(byteString: ByteString): CompletedGame {
      return byteString.parse { source ->
        CompletedGame(
            Ending.values()[source.readInt()],
            Turn.fromSnapshot(
                source.readByteStringWithLength()
            )
        )
      }
    }
  }
}

enum class Ending {
  Victory,
  Draw,
  Quitted
}
