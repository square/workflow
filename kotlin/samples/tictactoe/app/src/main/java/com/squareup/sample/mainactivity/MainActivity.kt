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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.IdlingResource
import com.squareup.sample.authworkflow.AuthViewFactories
import com.squareup.sample.container.SampleContainers
import com.squareup.sample.gameworkflow.TicTacToeViewFactories
import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener
import com.squareup.workflow.diagnostic.andThen
import com.squareup.workflow.diagnostic.tracing.TracingDiagnosticListener
import com.squareup.workflow.ui.WorkflowRunner
import com.squareup.workflow.ui.backstack.BackStackContainer
import com.squareup.workflow.ui.modal.AlertContainer
import com.squareup.workflow.ui.plus
import com.squareup.workflow.ui.setContentWorkflow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {
  private lateinit var component: MainComponent

  /** Exposed for use by espresso tests. */
  lateinit var idlingResource: IdlingResource

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // TODO: https://github.com/square/workflow/issues/603 Remove use of deprecated property.
    @Suppress("DEPRECATION")
    component = lastCustomNonConfigurationInstance as? MainComponent
        ?: MainComponent()

    idlingResource = component.idlingResource

    val traceFile = getExternalFilesDir(null)?.resolve("workflow-trace-tictactoe.json")!!

    val workflowRunner = setContentWorkflow(
        registry = viewRegistry,
        configure = {
          WorkflowRunner.Config(
              component.mainWorkflow,
              diagnosticListener = object : SimpleLoggingDiagnosticListener() {
                override fun println(text: String) = Timber.v(text)
              }.andThen(TracingDiagnosticListener(traceFile))
          )
        },
        // The sample MainWorkflow emits a Unit output when it is done, which means it's
        // time to end the activity.
        onResult = { finish() }
    )

    lifecycleScope.launch {
      workflowRunner.renderings.collect {
        Timber.d("rendering: %s", it)
      }
    }
  }

  override fun onRetainCustomNonConfigurationInstance(): Any = component

  private companion object {
    val viewRegistry = SampleContainers +
        AuthViewFactories +
        TicTacToeViewFactories +
        BackStackContainer +
        AlertContainer
  }
}
