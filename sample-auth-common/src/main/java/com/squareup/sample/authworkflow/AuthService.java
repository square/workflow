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
package com.squareup.sample.authworkflow;

import io.reactivex.Single;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class AuthService {
  private static final int DELAY_MILLIS = 750;
  private static final String WEAK_TOKEN = "need a second factor there, friend";
  private static final String REAL_TOKEN = "welcome aboard!";
  private static final String SECOND_FACTOR = "1234";

  static final class AuthRequest {
    final String email;
    final String password;

    AuthRequest(String email, String password) {
      this.email = email;
      this.password = password;
    }
  }

  public static final class AuthResponse {
    final String errorMessage;
    final String token;
    final boolean twoFactorRequired;

    AuthResponse(String errorMessage, String token, boolean twoFactorRequired) {
      this.errorMessage = errorMessage;
      this.token = token;
      this.twoFactorRequired = twoFactorRequired;
    }
  }

  Single<AuthResponse> login(AuthRequest request) {
    if (!"password".equals(request.password)) {
      return response(new AuthResponse("Unknown email or invalid password", "", false));
    }

    if (request.email.contains("2fa")) {
      return response(new AuthResponse("", WEAK_TOKEN, true));
    }

    return response(new AuthResponse("", REAL_TOKEN, false));
  }

  static final class SecondFactorRequest {
    final String token;
    final String secondFactor;

    SecondFactorRequest(String token, String secondFactor) {
      this.token = token;
      this.secondFactor = secondFactor;
    }
  }

  Single<AuthResponse> secondFactor(SecondFactorRequest request) {
    if (!WEAK_TOKEN.equals(request.token)) {
      return response(
          new AuthResponse("404!! What happened to your token there bud?!?!", "", false));
    }

    if (!SECOND_FACTOR.equals(request.secondFactor)) {
      return response(
          new AuthResponse(format("Invalid second factor (try %s)", SECOND_FACTOR), WEAK_TOKEN,
              true));
    }

    return response(new AuthResponse("", REAL_TOKEN, false));
  }

  private static <R> Single<R> response(R response) {
    return Single.just(response).delay(DELAY_MILLIS, TimeUnit.MILLISECONDS);
  }
}
