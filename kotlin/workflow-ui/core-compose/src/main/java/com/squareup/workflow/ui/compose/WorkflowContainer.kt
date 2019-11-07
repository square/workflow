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
@file:Suppress("EXPERIMENTAL_API_USAGE", "RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.workflow.ui.compose

import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.CoroutineContextAmbient
import com.squareup.workflow.OutputHandler
import com.squareup.workflow.Workflow
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.launchWorkflowForCompose
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Render a [Workflow]'s renderings.
 *
 * When this function is first composed it will start a new runtime. This runtime will be restarted
 * any time [workflow], [diagnosticListener], [immediateDispatcher], or the `CoroutineContext`
 * changes. The runtime will be cancelled when this function stops composing.
 *
 * @param workflow The [Workflow] to render.
 * @param props The props to render the root workflow with. If this value changes between calls,
 * the workflow runtime will re-render with the new props.
 * @param onOutput A function that will be invoked any time the root workflow emits an output.
 * @param diagnosticListener A [WorkflowDiagnosticListener] to configure on the runtime.
 * @param immediateDispatcher The [CoroutineDispatcher] to use for the workflow runtime. This will
 * always override any dispatcher in the current coroutine context.
 * @param children A [Composable] function that gets executed every time the root workflow spits
 * out a new rendering.
 */
@Composable
fun <P, O : Any, R : Any> WorkflowContainer(
  workflow: Workflow<P, O, R>,
  props: P,
  onOutput: (O) -> Unit,
  diagnosticListener: WorkflowDiagnosticListener? = null,
  immediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
  children: @Composable() (rendering: R) -> Unit
) {
  val rendering =
    observeWorkflow(workflow, props, onOutput, diagnosticListener, immediateDispatcher)
  rendering?.let { children(it) }
}

@Composable
private fun <P, O : Any, R : Any> observeWorkflow(
  workflow: Workflow<P, O, R>,
  props: P,
  onOutput: (O) -> Unit,
  diagnosticListener: WorkflowDiagnosticListener? = null,
  immediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): R? {
  val outputHandler = remember { OutputHandler<O>() }
  val context = ambient(CoroutineContextAmbient)
  val propsChannel = remember { Channel<P>(capacity = CONFLATED) }
  var rendering by state<R?> { null }

  // Set the output before starting the runtime so immediate outputs get reported.
  outputHandler.onOutput = onOutput
  propsChannel.offer(props)

  // Don't memoize onOutput props, we don't want to restart the runtime on every commit
  // or props change.
  onCommit(context, immediateDispatcher, workflow, diagnosticListener) {
    val scope = CoroutineScope(context + immediateDispatcher)
    val propsFlow = propsChannel.consumeAsFlow()
        .distinctUntilChanged()

    val session = launchWorkflowForCompose(
        scope, workflow, propsFlow,
        onRendering = { rendering = it },
        outputHandler = outputHandler,
        diagnosticListener = diagnosticListener
    )

    // Kill the runtime when we stop being composed.
    onDispose {
      session.cancel()
    }
  }

  return rendering
}
