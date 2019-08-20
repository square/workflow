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
import com.squareup.sample.authworkflow.LoginScreen.Event.Cancel
import com.squareup.sample.authworkflow.LoginScreen.Event.SubmitLogin
import com.squareup.sample.authworkflow.Result.Authorized
import com.squareup.sample.authworkflow.Result.Canceled
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.CancelSecondFactor
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.SubmitSecondFactor
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.runningWorker
import com.squareup.workflow.rx2.asWorker
import com.squareup.workflow.ui.BackStackScreen

/**
 * We define this otherwise redundant typealias to keep composite workflows
 * that build on [AuthWorkflow] decoupled from it, for ease of testing.
 */
typealias AuthWorkflow = Workflow<Unit, Result, BackStackScreen<*>>

sealed class Result {
  data class Authorized(val token: String) : Result()
  object Canceled : Result()
}

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
    StatefulWorkflow<Unit, AuthState, Result, BackStackScreen<*>>() {

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): AuthState = LoginPrompt()

  override fun render(
    input: Unit,
    state: AuthState,
    context: RenderContext<AuthState, Result>
  ): BackStackScreen<*> {
    return when (state) {
      is LoginPrompt -> {
        BackStackScreen(
            LoginScreen(
                state.errorMessage,
                context.onEvent { event ->
                  when {
                    event is SubmitLogin && event.isValidLoginRequest ->
                      enterState(Authorizing(event))
                    event is SubmitLogin -> enterState(LoginPrompt(event.userInputErrorMessage))
                    event === Cancel -> emitOutput(Canceled)
                    else -> throw IllegalStateException("Unknown event $event")
                  }
                }
            ),
            onGoBack = context.onEvent {
              emitOutput(Canceled)
            }
        )
      }

      is Authorizing -> {
        context.runningWorker(
            authService.login(AuthRequest(state.loginInfo.email, state.loginInfo.password))
                .asWorker()
        ) { response ->
          when {
            response.isLoginFailure -> enterState(LoginPrompt(response.errorMessage))
            response.twoFactorRequired -> enterState(SecondFactorPrompt(response.token))
            else -> emitOutput(Authorized(response.token))
          }
        }

        BackStackScreen(
            LoginScreen(),
            AuthorizingScreen("Logging in…")
        )
      }

      is SecondFactorPrompt -> {
        BackStackScreen(
            LoginScreen(),
            SecondFactorScreen(
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
        context.runningWorker(authService.secondFactor(request).asWorker()) { response ->
          when {
            response.isSecondFactorFailure ->
              enterState(SecondFactorPrompt(state.tempToken, response.errorMessage))
            else -> emitOutput(Authorized(response.token))
          }
        }

        BackStackScreen(
            LoginScreen(),
            SecondFactorScreen(),
            AuthorizingScreen("Submitting one time token…")
        )
      }
    }
  }

  /**
   * It'd be silly to restore an in progress login session, so saves nothing.
   */
  override fun snapshotState(state: AuthState): Snapshot = Snapshot.EMPTY

  private val AuthResponse.isLoginFailure: Boolean
    get() = token.isEmpty() && errorMessage.isNotEmpty()

  private val SubmitLogin.isValidLoginRequest: Boolean
    get() = userInputErrorMessage.isBlank()

  private val AuthResponse.isSecondFactorFailure: Boolean
    get() = token.isNotEmpty() && errorMessage.isNotEmpty()

  private val SubmitLogin.userInputErrorMessage: String
    get() = if (email.indexOf('@') < 0) "Invalid address" else ""
}
