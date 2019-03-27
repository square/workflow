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

import com.squareup.sample.authworkflow.AuthService.AuthRequest
import com.squareup.sample.authworkflow.AuthService.AuthResponse
import com.squareup.sample.authworkflow.AuthService.SecondFactorRequest
import com.squareup.sample.authworkflow.AuthState.Authorizing
import com.squareup.sample.authworkflow.AuthState.AuthorizingSecondFactor
import com.squareup.sample.authworkflow.AuthState.LoginPrompt
import com.squareup.sample.authworkflow.AuthState.SecondFactorPrompt
import com.squareup.sample.authworkflow.LoginScreen.SubmitLogin
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.CancelSecondFactor
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.SubmitSecondFactor
import com.squareup.viewregistry.BackStackScreen
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.rx2.onSuccess

/**
 * We define this otherwise redundant interface to keep composite workflows
 * that build on [AuthWorkflow] decoupled from it, for ease of testing.
 */
interface AuthWorkflow : Workflow<Unit, String, BackStackScreen<*>>

/**
 * Runs a set of login screens and pretends to produce an auth token,
 * via a pretend [authService].
 *
 * Demonstrates both client side validation (email format, must include "@")
 * and server side validation (password is "password").
 *
 * Includes a 2fa path for email addresses that include the string "2fa".
 * Token is "1234".
 */
class RealAuthWorkflow(private val authService: AuthService) : AuthWorkflow,
    StatefulWorkflow<Unit, AuthState, String, BackStackScreen<*>>() {

  override fun initialState(input: Unit): AuthState = LoginPrompt()

  override fun compose(
    input: Unit,
    state: AuthState,
    context: WorkflowContext<AuthState, String>
  ): BackStackScreen<*> {
    return when (state) {
      is LoginPrompt -> {
        BackStackScreen(LoginScreen(
            state.errorMessage,
            context.onEvent { event ->
              when {
                event.isValidLoginRequest -> enterState(Authorizing(event))
                else -> enterState(LoginPrompt(event.userInputErrorMessage))
              }
            }
        ))
      }

      is Authorizing -> {
        context.onSuccess(
            authService.login(AuthRequest(state.loginInfo.email, state.loginInfo.password))
        ) { response ->
          when {
            response.isLoginFailure -> enterState(LoginPrompt(response.errorMessage))
            response.twoFactorRequired -> enterState(SecondFactorPrompt(response.token))
            else -> emitOutput(response.token)
          }
        }

        BackStackScreen(AuthorizingScreen("Logging in…"))
      }

      is SecondFactorPrompt -> {
        BackStackScreen(SecondFactorScreen(
            state.errorMessage,
            context.onEvent { event ->
              when (event) {
                is SubmitSecondFactor ->
                  enterState(AuthorizingSecondFactor(state.tempToken, event))

                CancelSecondFactor -> enterState(LoginPrompt())
              }
            }
        ))
      }

      is AuthorizingSecondFactor -> {
        val request = SecondFactorRequest(state.tempToken, state.event.secondFactor)
        context.onSuccess(authService.secondFactor(request)) { response ->
          when {
            response.isSecondFactorFailure ->
              enterState(SecondFactorPrompt(state.tempToken, response.errorMessage))
            else -> emitOutput(response.token)
          }
        }

        // We give this one a uniquing key so that it pushes rather than pops
        // the first Authorizing screen.
        BackStackScreen(AuthorizingScreen("Submitting one time token…"), "2fa")
      }
    }
  }

  /**
   * It'd be silly to restore an in progress login session, so saves nothing.
   */
  override fun snapshotState(state: AuthState): Snapshot = Snapshot.EMPTY

  /**
   * [snapshotState] saves nothing, so always start from the [initialState].
   */
  override fun restoreState(snapshot: Snapshot): AuthState = initialState(Unit)

  private val AuthResponse.isLoginFailure: Boolean
    get() = token.isEmpty() && errorMessage.isNotEmpty()

  private val SubmitLogin.isValidLoginRequest: Boolean
    get() = userInputErrorMessage.isBlank()

  private val AuthResponse.isSecondFactorFailure: Boolean
    get() = token.isNotEmpty() && errorMessage.isNotEmpty()

  private val SubmitLogin.userInputErrorMessage: String
    get() = if (email.indexOf('@') < 0) "Invalid address" else ""
}
