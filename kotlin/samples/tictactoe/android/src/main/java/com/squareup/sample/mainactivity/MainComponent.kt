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
package com.squareup.sample.mainactivity

import com.squareup.sample.authworkflow.AuthService
import com.squareup.sample.authworkflow.AuthWorkflow
import com.squareup.sample.authworkflow.RealAuthWorkflow
import com.squareup.sample.gameworkflow.RealGameLog
import com.squareup.sample.gameworkflow.RealRunGameWorkflow
import com.squareup.sample.gameworkflow.RealTakeTurnsWorkflow
import com.squareup.sample.gameworkflow.RunGameWorkflow
import com.squareup.sample.gameworkflow.TakeTurnsWorkflow
import com.squareup.sample.mainworkflow.MainWorkflow
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import timber.log.Timber

/**
 * Pretend generated code of a pretend DI framework.
 */
internal class MainComponent {

  private val authService = AuthService()

  private fun authWorkflow(): AuthWorkflow = RealAuthWorkflow(authService)

  private fun gameLog() = RealGameLog(mainThread())

  private fun gameWorkflow(): RunGameWorkflow = RealRunGameWorkflow(takeTurnsWorkflow(), gameLog())

  private fun takeTurnsWorkflow(): TakeTurnsWorkflow = RealTakeTurnsWorkflow()

  val mainWorkflow = MainWorkflow(authWorkflow(), gameWorkflow())

  companion object {
    init {
      Timber.plant(Timber.DebugTree())

      val stock = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        Timber.e(error)
        stock.uncaughtException(thread, error)
      }
    }
  }
}
