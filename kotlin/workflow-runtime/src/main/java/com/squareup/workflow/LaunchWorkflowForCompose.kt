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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow

import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * **DO NOT USE**
 *
 * Workaround for Compose compiler not yet supporting coroutines.
 * This can go away once that's fixed.
 */
fun <PropsT, OutputT : Any, RenderingT> launchWorkflowForCompose(
  scope: CoroutineScope,
  workflow: Workflow<PropsT, OutputT, RenderingT>,
  props: Flow<PropsT>,
  onRendering: (RenderingT) -> Unit,
  outputHandler: OutputHandler<OutputT>,
  diagnosticListener: WorkflowDiagnosticListener?,
  initialSnapshot: Snapshot? = null
): ComposeWorkflowSession = launchWorkflowIn(scope, workflow, props, initialSnapshot) { session ->
  val workflowScope = this
  session.diagnosticListener = diagnosticListener

  session.renderingsAndSnapshots
      .onEach { onRendering(it.rendering) }
      .launchIn(workflowScope)

  session.outputs
      .onEach { outputHandler.onOutput(it) }
      .launchIn(workflowScope)

  return@launchWorkflowIn ComposeWorkflowSession(
      cancel = { workflowScope.cancel() }
  )
}

class OutputHandler<OutputT> {
  var onOutput: (OutputT) -> Unit = {}
}

class ComposeWorkflowSession(
  val cancel: () -> Unit
)
