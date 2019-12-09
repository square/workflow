/*
 * Copyright 2019 Square Inc.
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
package com.squareup.sample.dungeon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.squareup.workflow.diagnostic.tracing.TracingDiagnosticListener
import com.squareup.workflow.ui.WorkflowRunner
import com.squareup.workflow.ui.setContentWorkflow

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Ignore config changes for now.
    val component = Component(applicationContext)

    val traceFile = getExternalFilesDir(null)?.resolve("workflow-trace-dungeon.json")!!
    setContentWorkflow(component.viewRegistry) {
      WorkflowRunner.Config(
          workflow = component.timeMachineWorkflow,
          props = "simple_maze.txt",
          diagnosticListener = TracingDiagnosticListener(traceFile)
      )
    }
  }
}
