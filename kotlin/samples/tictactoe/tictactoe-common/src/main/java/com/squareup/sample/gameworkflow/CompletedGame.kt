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

import com.squareup.workflow.Snapshot
import com.squareup.workflow.parse
import okio.ByteString

/**
 * The [lastTurn] of a tic tac toe game, and its [ending]. Serves as the
 * result type for [TakeTurnsWorkflow].
 */
data class CompletedGame(
  val ending: Ending,
  val lastTurn: Turn = Turn()
) {

  fun toSnapshot(): Snapshot {
    return Snapshot.write { sink ->
      sink.writeInt(ending.ordinal)
    }
  }

  companion object {
    fun fromSnapshot(byteString: ByteString): CompletedGame {
      return byteString.parse { source ->
        CompletedGame(
            Ending.values()[source.readInt()]
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
