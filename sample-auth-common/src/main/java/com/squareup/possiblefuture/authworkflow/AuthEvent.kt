package com.squareup.possiblefuture.authworkflow

/**
 * Events accepted by the [AuthReactor].
 */
sealed class AuthEvent

data class SubmitLogin(
  val email: String,
  val password: String
) : AuthEvent()

data class SubmitSecondFactor(val secondFactor: String) : AuthEvent()
