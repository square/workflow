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

import com.squareup.sample.authworkflow.LoginScreen.Event.SubmitLogin
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.SubmitSecondFactor

sealed class AuthState {

  internal data class LoginPrompt(val errorMessage: String = "") : AuthState()

  internal data class Authorizing(val loginInfo: SubmitLogin) : AuthState()

  internal data class SecondFactorPrompt(
    val tempToken: String,
    val errorMessage: String = ""
  ) : AuthState()

  internal data class AuthorizingSecondFactor(
    val tempToken: String,
    val event: SubmitSecondFactor
  ) : AuthState()
}
