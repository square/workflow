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
import com.googlecode.lanterna.screen.Screen.RefreshType.COMPLETE
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.asWorker
import com.squareup.workflow.launchWorkflowIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.selectUnbiased

/**
 * Hosts [Workflow]s that:
 *  - gets information about the terminal configuration as input
 *  - renders the text to display on the terminal
 *  - finishes by emitting an exit code that should be passed to [kotlin.system.exitProcess].
 *
 * @param ioDispatcher Defaults to [Dispatchers.IO] and is used to listen for key events using
 * blocking APIs.
 */
class TerminalWorkflowRunner(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  private val screen = DefaultTerminalFactory().createScreen()

  /**
   * Runs [workflow] until it emits an [ExitCode] and then returns it.
   */
  @OptIn(
      FlowPreview::class,
      ExperimentalCoroutinesApi::class,
      ObsoleteCoroutinesApi::class
  )
  // Some methods on screen are synchronized, which Kotlin detects as blocking and warns us about
  // when invoking from coroutines. This entire function is blocking however, so we don't care.
  @Suppress("BlockingMethodInNonBlockingContext")
  fun run(workflow: TerminalWorkflow): ExitCode = runBlocking {
    val keyStrokesChannel = screen.listenForKeyStrokesOn(this + ioDispatcher)
    val keyStrokesWorker = keyStrokesChannel.asWorker()
    val resizes = screen.terminal.listenForResizesOn(this)

    // Hide the cursor.
    screen.cursorPosition = null

    try {
      screen.startScreen()
      try {
        return@runBlocking runTerminalWorkflow(workflow, screen, keyStrokesWorker, resizes)
      } finally {
        screen.stopScreen()
      }
    } finally {
      // Cancel all the coroutines we started so the coroutineScope block will actually exit if no
      // exception was thrown.
      keyStrokesChannel.cancel()
      resizes.cancel()
    }
  }
}

@Suppress("BlockingMethodInNonBlockingContext")
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
private suspend fun runTerminalWorkflow(
  workflow: TerminalWorkflow,
  screen: TerminalScreen,
  keyStrokes: Worker<KeyStroke>,
  resizes: ReceiveChannel<TerminalSize>
): ExitCode = coroutineScope {
  var input = TerminalProps(screen.terminalSize.toSize(), keyStrokes)
  val inputs = ConflatedBroadcastChannel(input)

  // Use the result as the parent Job of the runtime coroutine so it gets cancelled automatically
  // if there's an error.
  @Suppress("DEPRECATION")
  val result =
    launchWorkflowIn(this, workflow, inputs.asFlow()) { session ->
      val renderings = session.renderingsAndSnapshots.map { it.rendering }
          .produceIn(this)

      launch {
        while (true) {
          val rendering = selectUnbiased<TerminalRendering> {
            resizes.onReceive {
              screen.doResizeIfNecessary()
                  ?.let {
                    // If the terminal was resized since the last iteration, we need to notify the
                    // workflow.
                    input = input.copy(size = it.toSize())
                  }

              // Publish config changes to the workflow.
              inputs.send(input)

              // Sending that new input invalidated the lastRendering, so we don't want to
              // re-iterate until we have a new rendering with a fresh event handler. It also
              // triggered a render pass, so we can just retrieve that immediately.
              return@onReceive renderings.receive()
            }

            renderings.onReceive { it }
          }

          screen.clear()
          screen.newTextGraphics()
              .apply {
                foregroundColor = rendering.textColor.toTextColor()
                backgroundColor = rendering.backgroundColor.toTextColor()
                rendering.text.lineSequence()
                    .forEachIndexed { index, line ->
                      putString(TOP_LEFT_CORNER.withRelativeRow(index), line)
                    }
              }

          screen.refresh(COMPLETE)
        }
      }

      return@launchWorkflowIn async { session.outputs.first() }
    }

  val exitCode = result.await()
  // If we don't cancel the workflow runtime explicitly, coroutineScope will hang waiting for it to
  // finish.
  coroutineContext.cancelChildren(
      CancellationException("TerminalWorkflowRunner completed with exit code $exitCode")
  )
  return@coroutineScope exitCode
}
