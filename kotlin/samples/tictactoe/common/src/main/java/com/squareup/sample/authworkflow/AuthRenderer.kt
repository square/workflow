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
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool

object AuthRenderer : Renderer<AuthState, AuthEvent, BackStackScreen<Any>> {
  override fun render(
    state: AuthState,
    workflow: WorkflowInput<AuthEvent>,
    workflows: WorkflowPool
  ): BackStackScreen<Any> = BackStackScreen(
      when (state) {
        is LoginPrompt -> LoginScreen(state.errorMessage, workflow::sendEvent)

        is Authorizing -> AuthorizingScreen("Logging in…")

        is AuthorizingSecondFactor -> AuthorizingScreen("Submitting one time token…")

        is SecondFactorPrompt -> SecondFactorScreen(state.errorMessage, workflow::sendEvent)
      }
  )
}
