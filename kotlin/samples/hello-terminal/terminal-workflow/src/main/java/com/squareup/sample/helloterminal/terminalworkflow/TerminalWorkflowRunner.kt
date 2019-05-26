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
package com.squareup.sample.helloterminal.terminalworkflow

import com.googlecode.lanterna.TerminalPosition.TOP_LEFT_CORNER
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor.ANSI
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.Screen.RefreshType.COMPLETE
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.WorkflowHost
import com.squareup.workflow.WorkflowHost.Update
import com.squareup.workflow.stateless
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.selectUnbiased
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * TODO write documentation.
 */
class TerminalWorkflowRunner(
  private val hostFactory: WorkflowHost.Factory = WorkflowHost.Factory(EmptyCoroutineContext),
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  private val screen = DefaultTerminalFactory().createScreen()

  @UseExperimental(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
  @Suppress("BlockingMethodInNonBlockingContext")
  suspend fun run(workflow: TerminalWorkflow): ExitCode = coroutineScope {
    val configs = Channel<TerminalConfig>(CONFLATED)
    val wrapperWorkflow =
      Workflow.stateless<TerminalConfig, ExitCode, TerminalRendering> { config ->
        val originalRendering = renderChild(workflow, config) { emitOutput(it) }
        return@stateless originalRendering.copy(
            onKeyStroke = onEvent {
              println("3: ${it.source}")
              originalRendering.onKeyStroke(it)
              noop()
            }
        )
      }
    val host = hostFactory.run(wrapperWorkflow, configs, context = coroutineContext)
    val keyStrokes = screen.listenForKeyStrokesOn(this)
    val resizes = screen.terminal.listenForResizesOn(this)

    try {
      withContext(ioDispatcher) {
        screen.startScreen()
        try {
          runTerminalWorkflow(screen, configs, host, keyStrokes, resizes)
        } finally {
          screen.stopScreen()
        }
      }
    } finally {
      // Cancel all the coroutines we started so the coroutineScope block will actually exit if no
      // exception was thrown.
      host.updates.cancel()
      keyStrokes.cancel()
      resizes.cancel()
    }
  }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun runTerminalWorkflow(
  screen: Screen,
  configs: SendChannel<TerminalConfig>,
  host: WorkflowHost<TerminalConfig, ExitCode, TerminalRendering>,
  keyStrokes: ReceiveChannel<KeyStroke>,
  resizes: ReceiveChannel<TerminalSize>
): ExitCode {
  var config = TerminalConfig(screen.terminalSize.toSize())
  var lastRendering: TerminalRendering? = null

  // Need to send an initial input for the workflow to start running.
  configs.offer(config)

  while (true) {
    val update = selectUnbiased<Update<ExitCode, TerminalRendering>> {
      keyStrokes.onReceive { key ->
        println("2: $key")
        lastRendering?.onKeyStroke?.invoke(key.toKeyStroke())

        // Sending that event invalidated the lastRendering, so we don't want to re-iterate
        // until we have a new rendering with a fresh event handler. It also triggered a
        // render pass, so we can just retrieve that immediately.
        return@onReceive host.updates.receive()
      }

      resizes.onReceive {
        screen.doResizeIfNecessary()
            ?.let {
              // If the terminal was resized since the last iteration, we need to notify the
              // workflow.
              config = config.copy(size = it.toSize())
            }

        // Publish config changes to the workflow.
        configs.send(config)

        // Sending that new input invalidated the lastRendering, so we don't want to
        // re-iterate until we have a new rendering with a fresh event handler. It also
        // triggered a render pass, so we can just retrieve that immediately.
        return@onReceive host.updates.receive()
      }

      host.updates.onReceive { it }
    }

    // Stop the runner and return the exit code as soon as the workflow emits one.
    update.output?.let { exitCode ->
      return exitCode
    }

    screen.clear()
    screen.newTextGraphics()
        .apply {
          foregroundColor = ANSI.GREEN
          update.rendering.text.lineSequence()
              .forEachIndexed { index, line ->
                putString(TOP_LEFT_CORNER.withRelativeRow(index), line)
              }
        }

    screen.refresh(COMPLETE)
    lastRendering = update.rendering
  }
}
