package com.squareup.sample.authworkflow

import io.reactivex.Single

interface AuthService {
  fun login(request: AuthRequest): Single<AuthResponse>

  fun secondFactor(request: SecondFactorRequest): Single<AuthResponse>

  data class AuthRequest(
    val email: String,
    val password: String
  )

  data class AuthResponse(
    val errorMessage: String,
    val token: String,
    val twoFactorRequired: Boolean
  )

  data class SecondFactorRequest(
    val token: String,
    val secondFactor: String
  )
}
