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

import com.squareup.sample.authworkflow.Action.CancelLogin
import com.squareup.sample.authworkflow.Action.CancelSecondFactor
import com.squareup.sample.authworkflow.Action.HandleAuthResponse
import com.squareup.sample.authworkflow.Action.HandleSecondFactorResponse
import com.squareup.sample.authworkflow.Action.SubmitLogin
import com.squareup.sample.authworkflow.Action.SubmitSecondFactor
import com.squareup.sample.authworkflow.AuthResult.Authorized
import com.squareup.sample.authworkflow.AuthResult.Canceled
import com.squareup.sample.authworkflow.AuthService.AuthRequest
import com.squareup.sample.authworkflow.AuthService.AuthResponse
import com.squareup.sample.authworkflow.AuthService.SecondFactorRequest
import com.squareup.sample.authworkflow.AuthState.Authorizing
import com.squareup.sample.authworkflow.AuthState.AuthorizingSecondFactor
import com.squareup.sample.authworkflow.AuthState.LoginPrompt
import com.squareup.sample.authworkflow.AuthState.SecondFactorPrompt
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.rx2.asWorker
import com.squareup.workflow.ui.backstack.BackStackScreen

/**
 * We define this otherwise redundant typealias to keep composite workflows
 * that build on [AuthWorkflow] decoupled from it, for ease of testing.
 */
typealias AuthWorkflow = Workflow<Unit, AuthResult, BackStackScreen<Any>>

sealed class AuthState {
  internal data class LoginPrompt(val errorMessage: String = "") : AuthState()

  internal data class Authorizing(
    val email: String,
    val password: String
  ) : AuthState()

  internal data class SecondFactorPrompt(
    val tempToken: String,
    val errorMessage: String = ""
  ) : AuthState()

  internal data class AuthorizingSecondFactor(
    val tempToken: String,
    val secondFactor: String
  ) : AuthState()
}

sealed class AuthResult {
  data class Authorized(val token: String) : AuthResult()
  object Canceled : AuthResult()
}

internal sealed class Action : WorkflowAction<AuthState, AuthResult> {
  class SubmitLogin(
    val email: String,
    val password: String
  ) : Action()

  object CancelLogin : Action()

  class HandleAuthResponse(val response: AuthResponse) : Action()

  class SubmitSecondFactor(
    val tempToken: String,
    val secondFactor: String
  ) : Action()

  object CancelSecondFactor : Action()

  class HandleSecondFactorResponse(
    val tempToken: String,
    val response: AuthResponse
  ) : Action()

  final override fun Updater<AuthState, AuthResult>.apply() {
    when (this@Action) {
      is SubmitLogin -> {
        nextState = when {
          email.isValidEmail -> Authorizing(email, password)
          else -> LoginPrompt(email.emailValidationErrorMessage)
        }
      }

      CancelLogin -> setOutput(Canceled)

      is HandleAuthResponse -> {
        when {
          response.isLoginFailure -> nextState = LoginPrompt(response.errorMessage)
          response.twoFactorRequired -> nextState = SecondFactorPrompt(response.token)
          else -> setOutput(Authorized(response.token))
        }
      }

      is SubmitSecondFactor -> nextState = AuthorizingSecondFactor(tempToken, secondFactor)

      CancelSecondFactor -> nextState = LoginPrompt()

      is HandleSecondFactorResponse -> {
        when {
          response.isSecondFactorFailure ->
            nextState = SecondFactorPrompt(tempToken, response.errorMessage)
          else -> setOutput(Authorized(response.token))
        }
      }
    }
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
class RealAuthWorkflow(private val authService: AuthService) : AuthWorkflow,
    StatefulWorkflow<Unit, AuthState, AuthResult, BackStackScreen<Any>>() {

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): AuthState = LoginPrompt()

  override fun render(
    props: Unit,
    state: AuthState,
    context: RenderContext<AuthState, AuthResult>
  ): BackStackScreen<Any> = when (state) {
    is LoginPrompt -> {
      BackStackScreen(
          LoginScreen(
              state.errorMessage,
              onLogin = { email, password -> context.actionSink.send(SubmitLogin(email, password)) },
              onCancel = { context.actionSink.send(CancelLogin) }
          )
      )
    }

    is Authorizing -> {
      context.runningWorker(
          authService.login(AuthRequest(state.email, state.password))
              .asWorker()
      ) { HandleAuthResponse(it) }

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
              onSubmit = { context.actionSink.send(SubmitSecondFactor(state.tempToken, it)) },
              onCancel = { context.actionSink.send(CancelSecondFactor) }
          )
      )
    }

    is AuthorizingSecondFactor -> {
      val request = SecondFactorRequest(state.tempToken, state.secondFactor)
      context.runningWorker(authService.secondFactor(request).asWorker()) {
        HandleSecondFactorResponse(state.tempToken, it)
      }

      BackStackScreen(
          LoginScreen(),
          SecondFactorScreen(),
          AuthorizingScreen("Submitting one time token…")
      )
    }
  }

  /**
   * It'd be silly to restore an in progress login session, so saves nothing.
   */
  override fun snapshotState(state: AuthState): Snapshot = Snapshot.EMPTY
}

private val AuthResponse.isLoginFailure: Boolean
  get() = token.isEmpty() && errorMessage.isNotEmpty()

private val AuthResponse.isSecondFactorFailure: Boolean
  get() = token.isNotEmpty() && errorMessage.isNotEmpty()

private val String.isValidEmail: Boolean
  get() = emailValidationErrorMessage.isBlank()

private val String.emailValidationErrorMessage: String
  get() = if (indexOf('@') < 0) "Invalid address" else ""
