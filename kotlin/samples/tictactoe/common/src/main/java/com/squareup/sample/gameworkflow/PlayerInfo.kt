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

import com.squareup.workflow.Snapshot
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.writeUtf8WithLength
import okio.ByteString

/**
 * Defines the names of the players of a Tic Tac Toe game.
 */
data class PlayerInfo(
  val xName: String = "",
  val oName: String = ""
) {
  fun toSnapshot(): Snapshot = Snapshot.write { sink ->
    sink.writeUtf8WithLength(xName)
    sink.writeUtf8WithLength(oName)
  }

  companion object {
    fun fromSnapshot(byteString: ByteString): PlayerInfo = byteString.parse {
      PlayerInfo(
          it.readUtf8WithLength(),
          it.readUtf8WithLength()
      )
    }
  }
}
