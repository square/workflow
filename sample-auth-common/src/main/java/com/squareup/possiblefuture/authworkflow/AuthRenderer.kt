package com.squareup.possiblefuture.authworkflow

import com.squareup.possiblefuture.authworkflow.AuthState.Authorizing
import com.squareup.possiblefuture.authworkflow.AuthState.AuthorizingSecondFactor
import com.squareup.possiblefuture.authworkflow.AuthState.LoginPrompt
import com.squareup.possiblefuture.authworkflow.AuthState.SecondFactorPrompt
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool

object AuthRenderer : Renderer<AuthState, AuthEvent, Any> {
  override fun render(
    state: AuthState,
    workflow: WorkflowInput<AuthEvent>,
    workflows: WorkflowPool
  ): Any {
    return when (state) {
      is LoginPrompt -> LoginScreen(state.errorMessage, workflow::sendEvent)

      is Authorizing -> AuthorizingScreen("Logging in…")

      is AuthorizingSecondFactor -> AuthorizingScreen("Submitting one time token…")

      is SecondFactorPrompt -> SecondFactorScreen(state.errorMessage, workflow::sendEvent)
    }
  }
}
