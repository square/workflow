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

import com.squareup.sample.authworkflow.AuthService.AuthResponse
import com.squareup.sample.authworkflow.AuthState.Authorizing
import com.squareup.sample.authworkflow.AuthState.AuthorizingSecondFactor
import com.squareup.sample.authworkflow.AuthState.LoginPrompt
import com.squareup.sample.authworkflow.AuthState.SecondFactorPrompt
import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Handle
import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.rx2.EventChannel
import com.squareup.workflow.rx2.Reactor
import com.squareup.workflow.rx2.doLaunch
import io.reactivex.Scheduler
import io.reactivex.Single

/**
 * We define this otherwise redundant interface to keep composite reactors
 * that build on [AuthReactor] decoupled from it, for ease of testing.
 */
interface AuthLauncher : Launcher<AuthState, AuthEvent, String> {
  companion object {
    fun handle(state: AuthState = AuthState.startingState()): Handle<AuthState, AuthEvent, String> =
      WorkflowPool.handle(AuthLauncher::class, state)
  }
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
class AuthReactor(
  private val authService: AuthService,
  private val main: Scheduler
) : Reactor<AuthState, AuthEvent, String>, AuthLauncher {

  override fun launch(
    initialState: AuthState,
    workflows: WorkflowPool
  ): Workflow<AuthState, AuthEvent, String> = doLaunch(initialState, workflows)

  override fun onReact(
    state: AuthState,
    events: EventChannel<AuthEvent>,
    workflows: WorkflowPool
  ): Single<out Reaction<AuthState, String>> {
    return when (state) {
      is LoginPrompt -> events.select { onEvent<SubmitLogin> { EnterState(trySubmitLogin(it)) } }
      is Authorizing -> doLogin(state.event)
      is SecondFactorPrompt -> events.select {
        onEvent<SubmitSecondFactor> { EnterState(AuthorizingSecondFactor(state.tempToken, it)) }
      }
      is AuthorizingSecondFactor -> doSecondFactor(state)
    }
  }

  // region State Handlers

  private fun trySubmitLogin(event: SubmitLogin): AuthState {
    return if (event.isValidLoginRequest) {
      Authorizing(event)
    } else {
      LoginPrompt(event.userInputErrorMessage)
    }
  }

  private fun doLogin(event: SubmitLogin): Single<Reaction<AuthState, String>> {
    return authService.login(AuthService.AuthRequest(event.email, event.password))
        .observeOn(main)
        .map { response ->
          when {
            response.isLoginFailure -> EnterState(LoginPrompt(response.errorMessage))
            response.twoFactorRequired -> {
              // When server asks for second factor, so do we.
              EnterState(SecondFactorPrompt(response.token))
            }
            else -> FinishWith(response.token)
          }
        }
  }

  private fun doSecondFactor(state: AuthorizingSecondFactor): Single<Reaction<AuthState, String>> {
    return authService.secondFactor(
        AuthService.SecondFactorRequest(state.tempToken, state.event.secondFactor)
    )
        .observeOn(main)
        .map { response ->
          when {
            response.isSecondFactorFailure -> {
              EnterState(SecondFactorPrompt(state.tempToken, response.errorMessage))
            }
            else -> FinishWith(response.token)
          }
        }
  }

  // endregion

  private val AuthResponse.isLoginFailure: Boolean
    get() = token.isEmpty() && errorMessage.isNotEmpty()

  private val SubmitLogin.isValidLoginRequest: Boolean
    get() = userInputErrorMessage.isBlank()

  private val AuthResponse.isSecondFactorFailure: Boolean
    get() = token.isNotEmpty() && errorMessage.isNotEmpty()

  private val SubmitLogin.userInputErrorMessage: String
    get() = if (email.indexOf('@') < 0) "Invalid address" else ""
}
