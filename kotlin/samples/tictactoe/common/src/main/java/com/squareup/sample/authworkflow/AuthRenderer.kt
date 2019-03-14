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

import com.squareup.sample.authworkflow.AuthState.Authorizing
import com.squareup.sample.authworkflow.AuthState.AuthorizingSecondFactor
import com.squareup.sample.authworkflow.AuthState.LoginPrompt
import com.squareup.sample.authworkflow.AuthState.SecondFactorPrompt
import com.squareup.viewregistry.BackStackScreen
import com.squareup.workflow.legacy.Renderer
import com.squareup.workflow.legacy.WorkflowInput
import com.squareup.workflow.legacy.WorkflowPool

object AuthRenderer : Renderer<AuthState, AuthEvent, BackStackScreen<*>> {
  override fun render(
    state: AuthState,
    workflow: WorkflowInput<AuthEvent>,
    workflows: WorkflowPool
  ): BackStackScreen<*> =
    when (state) {
      is LoginPrompt -> BackStackScreen(LoginScreen(state.errorMessage, workflow::sendEvent))

      is Authorizing -> BackStackScreen(AuthorizingScreen("Logging in…"))

      // We give this one a uniquing key so that it pushes rather than pops
      // the first Authorizing screen.
      is AuthorizingSecondFactor -> BackStackScreen(
          AuthorizingScreen("Submitting one time token…"),
          "2fa"
      )

      is SecondFactorPrompt -> BackStackScreen(
          SecondFactorScreen(state.errorMessage, workflow::sendEvent)
      )
    }
}
