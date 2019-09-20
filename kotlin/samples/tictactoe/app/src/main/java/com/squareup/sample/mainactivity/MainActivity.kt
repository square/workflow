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
import androidx.test.espresso.IdlingResource
import com.squareup.sample.authworkflow.AuthViewBindings
import com.squareup.sample.gameworkflow.TicTacToeViewBindings
import com.squareup.sample.panel.PanelContainer
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowRunner
import com.squareup.workflow.ui.setContentWorkflow
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import timber.log.Timber

class MainActivity : AppCompatActivity() {
  private var loggingSub = Disposables.disposed()

  private lateinit var component: MainComponent
  private lateinit var workflowRunner: WorkflowRunner<Unit>

  lateinit var idlingResource: IdlingResource

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // TODO: https://github.com/square/workflow/issues/603 Remove use of deprecated property.
    @Suppress("DEPRECATION")
    component = lastCustomNonConfigurationInstance as? MainComponent
        ?: MainComponent()

    idlingResource = component.idlingResource

    workflowRunner = setContentWorkflow(
        savedInstanceState,
        { WorkflowRunner.Config(component.mainWorkflow, viewRegistry) }
    ) {
      finish()
    }

    loggingSub = CompositeDisposable(
        workflowRunner.renderings.subscribe { Timber.d("rendering: %s", it) },
        workflowRunner.debugInfo.subscribe { Timber.v("debug snapshot: %s", it) }
    )
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    workflowRunner.onSaveInstanceState(outState)
  }

  override fun onRetainCustomNonConfigurationInstance(): Any = component

  override fun onDestroy() {
    loggingSub.dispose()
    super.onDestroy()
  }

  private companion object {
    val viewRegistry = ViewRegistry(PanelContainer) + AuthViewBindings + TicTacToeViewBindings
  }
}
