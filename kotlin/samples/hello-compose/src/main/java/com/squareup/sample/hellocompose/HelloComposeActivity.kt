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
package com.squareup.sample.hellocompose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBorder
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.dp
import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.compose.WorkflowContainer
import com.squareup.workflow.ui.compose.showRendering

private val viewRegistry = ViewRegistry(HelloBinding)
private val containerHints = ContainerHints(viewRegistry)

class HelloComposeActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        WorkflowContainer(
            workflow = HelloWorkflow,
            props = Unit,
            diagnosticListener = SimpleLoggingDiagnosticListener(),
            onOutput = {}
        ) { rendering ->
          Box(
              modifier = Modifier.drawBorder(
                  shape = RoundedCornerShape(10.dp),
                  size = 10.dp,
                  color = Color.Magenta
              )
          ) {
            containerHints.showRendering(rendering)
          }
        }
      }
    }
  }
}
