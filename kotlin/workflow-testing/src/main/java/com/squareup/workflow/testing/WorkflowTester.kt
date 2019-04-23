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

package com.squareup.workflow.testing

import com.squareup.workflow.Snapshot
import com.squareup.workflow.WorkflowHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext

/**
 * Runs a [Workflow][com.squareup.workflow.Workflow] and provides access to its
 * [renderings][awaitNextRendering], [outputs][awaitNextOutput], and [snapshots][awaitNextSnapshot].
 *
 * For each of renderings, outputs, and snapshots, this class gives you a few ways to access
 * information about them:
 *  - [awaitNextRendering], [awaitNextOutput], [awaitNextSnapshot]
 *    - Block until something becomes available, and then return it.
 *  - [withNextRendering], [withNextOutput], [withNextSnapshot]
 *    - Block until something becomes available, and then pass it to a lambda.
 *  - [hasRendering], [hasOutput], [hasSnapshot]
 *    - Return `true` if the previous methods won't block.
 */
class WorkflowTester<InputT : Any, OutputT : Any, RenderingT : Any> @TestOnly internal constructor(
  private val inputs: SendChannel<InputT>,
  private val host: WorkflowHost<InputT, OutputT, RenderingT>,
  context: CoroutineContext
) {

  private val renderings = Channel<RenderingT>(capacity = UNLIMITED)
  private val snapshots = Channel<Snapshot>(capacity = UNLIMITED)
  private val outputs = Channel<OutputT>(capacity = UNLIMITED)

  init {
    val job = CoroutineScope(context)
        .launch {
          host.updates.consumeEach { (rendering, snapshot, output) ->
            renderings.send(rendering)
            snapshots.send(snapshot)
            output?.let { outputs.send(it) }
          }
        }
    job.invokeOnCompletion { cause ->
      renderings.close(cause)
      snapshots.close(cause)
      outputs.close(cause)
    }
  }

  /**
   * True if the workflow has emitted a new rendering that is ready to be consumed.
   */
  val hasRendering: Boolean get() = !renderings.isEmptyOrClosed

  /**
   * True if the workflow has emitted a new snapshot that is ready to be consumed.
   */
  val hasSnapshot: Boolean get() = !snapshots.isEmptyOrClosed

  /**
   * True if the workflow has emitted a new output that is ready to be consumed.
   */
  val hasOutput: Boolean get() = !outputs.isEmptyOrClosed

  private val ReceiveChannel<*>.isEmptyOrClosed get() = isEmpty || isClosedForReceive

  /**
   * Sends [input] to the workflow-under-test.
   */
  fun sendInput(input: InputT) {
    runBlocking {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        inputs.send(input)
      }
    }
  }

  /**
   * Blocks until the workflow emits a rendering, then returns it.
   *
   * @param timeoutMs The maximum amount of time to wait for a rendering to be emitted. If null,
   * [WorkflowTester.DEFAULT_TIMEOUT_MS] will be used instead.
   * @param skipIntermediate If true, and the workflow has emitted multiple renderings, all but the
   * most recent one will be dropped.
   */
  fun awaitNextRendering(
    timeoutMs: Long? = null,
    skipIntermediate: Boolean = true
  ): RenderingT = renderings.receiveBlocking(timeoutMs, skipIntermediate)

  /**
   * Blocks until the workflow emits a rendering, then passes it to [block].
   *
   * @param timeoutMs The maximum amount of time to wait for a rendering to be emitted. If null,
   * [WorkflowTester.DEFAULT_TIMEOUT_MS] will be used instead.
   * @param skipIntermediate If true, and the workflow has emitted multiple renderings, all but the
   * most recent one will be dropped.
   * @return The value returned from [block].
   */
  fun <T> withNextRendering(
    timeoutMs: Long? = null,
    skipIntermediate: Boolean = true,
    block: (RenderingT) -> T
  ): T = awaitNextRendering(timeoutMs, skipIntermediate).let(block)

  /**
   * Blocks until the workflow emits a snapshot, then returns it.
   *
   * @param timeoutMs The maximum amount of time to wait for a snapshot to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   * @param skipIntermediate If true, and the workflow has emitted multiple snapshots, all but the
   * most recent one will be dropped.
   */
  fun awaitNextSnapshot(
    timeoutMs: Long? = null,
    skipIntermediate: Boolean = true
  ): Snapshot = snapshots.receiveBlocking(timeoutMs, skipIntermediate)

  /**
   * Blocks until the workflow emits a snapshot, then passes it to [block].
   *
   * @param timeoutMs The maximum amount of time to wait for a snapshot to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   * @param skipIntermediate If true, and the workflow has emitted multiple snapshots, all but the
   * most recent one will be dropped.
   * @return The value returned from [block].
   */
  fun <T> withNextSnapshot(
    timeoutMs: Long? = null,
    skipIntermediate: Boolean = true,
    block: (Snapshot) -> T
  ) = awaitNextSnapshot(timeoutMs, skipIntermediate).let(block)

  /**
   * Blocks until the workflow emits an output, then returns it.
   *
   * @param timeoutMs The maximum amount of time to wait for an output to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   */
  fun awaitNextOutput(timeoutMs: Long? = null): OutputT =
    outputs.receiveBlocking(timeoutMs, drain = false)

  /**
   * Blocks until the workflow emits an output, then passes it to [block].
   *
   * @param timeoutMs The maximum amount of time to wait for an output to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   * @return The value returned from [block].
   */
  fun <T> withNextOutput(
    timeoutMs: Long? = null,
    block: (OutputT) -> T
  ): T = awaitNextOutput(timeoutMs).let(block)

  /**
   * Blocks until the workflow fails by throwing an exception, then returns that exception.
   *
   * @param timeoutMs The maximum amount of time to wait for an output to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   */
  fun awaitFailure(timeoutMs: Long? = null): Throwable {
    var error: Throwable? = null
    runBlocking {
      withTimeout(timeoutMs ?: DEFAULT_TIMEOUT_MS) {
        try {
          while (true) renderings.receive()
        } catch (e: Throwable) {
          error = e
        }
      }
    }
    return error!!
  }

  /**
   * Blocks until the workflow fails by throwing an exception, then passes that exception to
   * [block].
   *
   * @param timeoutMs The maximum amount of time to wait for an output to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   * @return The value returned from [block].
   */
  fun <T> withFailure(
    timeoutMs: Long? = null,
    block: (Throwable) -> T
  ): T = awaitFailure(timeoutMs).let(block)

  /**
   * @param drain If true, this function will consume all the values currently in the channel, and
   * return the last one.
   */
  private fun <T> ReceiveChannel<T>.receiveBlocking(
    timeoutMs: Long?,
    drain: Boolean
  ): T = runBlocking {
    withTimeout(timeoutMs ?: DEFAULT_TIMEOUT_MS) {
      var item = receive()
      if (drain) {
        while (!isEmpty) {
          item = receive()
        }
      }
      return@withTimeout item
    }
  }

  companion object {
    const val DEFAULT_TIMEOUT_MS: Long = 5000
  }
}
