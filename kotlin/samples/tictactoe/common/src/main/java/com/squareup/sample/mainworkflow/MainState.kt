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
package com.squareup.sample.mainworkflow

import com.squareup.workflow.Snapshot
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.writeUtf8WithLength
import okio.ByteString

/**
 * The state of [MainWorkflow]. Indicates which nested workflow is running, and records
 * the current nested state.
 */
sealed class MainState {

  internal object Authenticating : MainState()

  internal object RunningGame : MainState()

  fun toSnapshot(): Snapshot {
    return Snapshot.write { sink -> sink.writeUtf8WithLength(this::class.java.name) }
  }

  companion object {
    fun fromSnapshot(byteString: ByteString): MainState = byteString.parse {
      val mainStateName = it.readUtf8WithLength()

      return when (mainStateName) {
        Authenticating::class.java.name -> Authenticating
        RunningGame::class.java.name -> RunningGame
        else -> throw IllegalArgumentException("Unrecognized state: $mainStateName")
      }
    }
  }
}
