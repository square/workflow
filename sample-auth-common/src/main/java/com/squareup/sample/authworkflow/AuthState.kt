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
package com.squareup.sample.authworkflow

import com.squareup.workflow.Snapshot
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.writeUtf8WithLength
import okio.ByteString

sealed class AuthState {

  internal data class LoginPrompt(val errorMessage: String = "") : AuthState()

  internal data class Authorizing(val event: SubmitLogin) : AuthState()

  internal data class SecondFactorPrompt(
    val tempToken: String,
    val errorMessage: String = ""
  ) : AuthState()

  internal data class AuthorizingSecondFactor(
    val tempToken: String,
    val event: SubmitSecondFactor
  ) : AuthState()

  fun toSnapshot() = Snapshot.write {
    // Production code could be more judicious here (e.g., don't save the Authorizing state),
    // and more complete (e.g. save the current error message, save the token). But this
    // is enough to prove the point.
    when (this) {
      is LoginPrompt, is Authorizing -> {
        // We don't want to save any passwords, so if we snapshot in the middle of
        // authorizing we'll just return to the login prompt.
        it.writeInt(LOGIN_PROMPT_TAG)
      }
      is SecondFactorPrompt -> {
        it.writeInt(SECOND_FACTOR_PROMPT_TAG)
        it.writeUtf8WithLength(this.tempToken)
      }
      is AuthorizingSecondFactor -> {
        it.writeInt(SECOND_FACTOR_PROMPT_TAG)
        it.writeUtf8WithLength(this.tempToken)
      }
    }
  }

  companion object {
    fun start(): AuthState = LoginPrompt()

    // Tags used to serialize the state.
    internal const val LOGIN_PROMPT_TAG = 0
    internal const val SECOND_FACTOR_PROMPT_TAG = 1

    fun fromSnapshot(byteString: ByteString): AuthState = byteString.parse {
      val tag = it.readInt()
      return when (tag) {
        LOGIN_PROMPT_TAG -> LoginPrompt()
        SECOND_FACTOR_PROMPT_TAG -> SecondFactorPrompt(tempToken = it.readUtf8WithLength())
        else -> throw IllegalArgumentException("Invalid tag: $tag")
      }
    }
  }
}
