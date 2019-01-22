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
package com.squareup.sample.authgameapp

import com.squareup.sample.authworkflow.AuthReactor
import com.squareup.sample.authworkflow.AuthService
import com.squareup.sample.tictactoe.RealGameLog
import com.squareup.sample.tictactoe.RunGameReactor
import com.squareup.sample.tictactoe.TakeTurnsReactor
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPoolMonitor
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitor
import com.squareup.workflow.monitoring.webview.WebViewWorkflowPoolMonitor
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import timber.log.Timber

/**
 * Pretend generated code of a pretend DI framework.
 */
internal class ShellComponent {

  val workflowTracer = TracingWorkflowPoolMonitor()

  val workflowWebview = WebViewWorkflowPoolMonitor(8123, workflowTracer)

  val workflowPoolMonitor: WorkflowPoolMonitor = WorkflowPoolMonitor.merge(
      TimberWorkflowPoolMonitor(),
      workflowTracer,
      workflowWebview
  )

  val workflowPool = WorkflowPool(workflowPoolMonitor)

  private val authService = AuthService()

  fun shellReactor() = ShellReactor(gameReactor(), authReactor())

  private fun authReactor() = AuthReactor(authService, mainThread())

  private fun gameLog() = RealGameLog(mainThread())

  private fun gameReactor() = RunGameReactor(takeTurnsReactor(), gameLog())

  private fun takeTurnsReactor() = TakeTurnsReactor()

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
