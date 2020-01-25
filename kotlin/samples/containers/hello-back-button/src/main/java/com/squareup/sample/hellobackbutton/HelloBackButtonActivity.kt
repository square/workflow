/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.hellobackbutton

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.squareup.sample.container.SampleContainers
import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowRunner
import com.squareup.workflow.ui.modal.AlertContainer
import com.squareup.workflow.ui.plus
import com.squareup.workflow.ui.setContentWorkflow

private val viewRegistry =
  ViewRegistry(HelloBackButtonLayoutRunner) + SampleContainers + AlertContainer

class HelloBackButtonActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentWorkflow(
        viewRegistry,
        configure = {
          WorkflowRunner.Config(
              AreYouSureWorkflow,
              diagnosticListener = SimpleLoggingDiagnosticListener()
          )
        },
        onResult = { finish() }
    )
  }
}
