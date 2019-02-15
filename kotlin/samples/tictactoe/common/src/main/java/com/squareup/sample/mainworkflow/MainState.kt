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

import com.squareup.sample.authworkflow.AuthEvent
import com.squareup.sample.authworkflow.AuthLauncher
import com.squareup.sample.authworkflow.AuthState
import com.squareup.sample.gameworkflow.RunGameEvent
import com.squareup.sample.gameworkflow.RunGameLauncher
import com.squareup.sample.gameworkflow.RunGameResult
import com.squareup.sample.gameworkflow.RunGameState
import com.squareup.workflow.Snapshot
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.parse
import com.squareup.workflow.readByteStringWithLength
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.writeByteStringWithLength
import com.squareup.workflow.writeUtf8WithLength
import okio.ByteString
import kotlin.reflect.jvm.jvmName

/**
 * The state of [MainReactor]. Indicates which nested workflow is running, and records
 * the current nested state.
 */
sealed class MainState {

  internal data class Authenticating(
    val authWorkflow: WorkflowPool.Handle<AuthState, AuthEvent, String> = AuthLauncher.handle()
  ) : MainState()

  internal data class RunningGame(
    val runGameWorkflow: WorkflowPool.Handle<RunGameState, RunGameEvent, RunGameResult> =
      RunGameLauncher.handle()
  ) : MainState()

  fun toSnapshot(): Snapshot {
    return Snapshot.write { sink ->
      sink.writeUtf8WithLength(this::class.jvmName)

      when (this) {
        is Authenticating -> sink.writeByteStringWithLength(authWorkflow.state.toSnapshot().bytes)
        is RunningGame -> sink.writeByteStringWithLength(runGameWorkflow.state.toSnapshot().bytes)
      }
    }
  }

  companion object {
    fun startingState(): MainState =
      Authenticating()

    fun fromSnapshot(byteString: ByteString): MainState = byteString.parse {
      val mainStateName = it.readUtf8WithLength()
      val delegateByteString = it.readByteStringWithLength()

      return when (mainStateName) {
        Authenticating::class.jvmName ->
          Authenticating(
              AuthLauncher.handle(AuthState.fromSnapshot(delegateByteString))
          )
        RunningGame::class.jvmName ->
          RunningGame(
              RunGameLauncher.handle(
                  RunGameState.fromSnapshot(delegateByteString)
              )
          )

        else -> throw IllegalArgumentException("Unrecognized state: $mainStateName")
      }
    }
  }
}
