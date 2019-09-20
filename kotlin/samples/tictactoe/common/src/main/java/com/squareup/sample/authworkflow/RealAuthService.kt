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
import io.reactivex.Single
import java.lang.String.format
import java.util.concurrent.TimeUnit

class RealAuthService : AuthService {

  override fun login(request: AuthRequest): Single<AuthResponse> {
    return when {
      "password" != request.password -> response(
          AuthResponse("Unknown email or invalid password", "", false)
      )
      request.email.contains("2fa") -> response(AuthResponse("", WEAK_TOKEN, true))
      else -> response(AuthResponse("", REAL_TOKEN, false))
    }
  }

  override fun secondFactor(request: SecondFactorRequest): Single<AuthResponse> {
    return when {
      WEAK_TOKEN != request.token -> response(
          AuthResponse("404!! What happened to your token there bud?!?!", "", false)
      )
      SECOND_FACTOR != request.secondFactor -> response(
          AuthResponse(
              format("Invalid second factor (try %s)", SECOND_FACTOR), WEAK_TOKEN,
              true
          )
      )
      else -> response(AuthResponse("", REAL_TOKEN, false))
    }
  }

  companion object {
    private const val DELAY_MILLIS = 750
    private const val WEAK_TOKEN = "need a second factor there, friend"
    private const val REAL_TOKEN = "welcome aboard!"
    private const val SECOND_FACTOR = "1234"

    private fun <R> response(response: R): Single<R> {
      return Single.just(response)
          .delay(DELAY_MILLIS.toLong(), TimeUnit.MILLISECONDS)
    }
  }
}
