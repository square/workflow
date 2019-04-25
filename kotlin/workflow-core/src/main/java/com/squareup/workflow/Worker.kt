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
package com.squareup.workflow

import com.squareup.workflow.Worker.Companion.create
import com.squareup.workflow.Worker.Companion.from
import com.squareup.workflow.Worker.Companion.fromChannel
import com.squareup.workflow.Worker.Companion.fromNullable
import com.squareup.workflow.Worker.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass

/**
 * Represents a unit of asynchronous work that can have zero, one, or multiple outputs.
 *
 * A [Workflow] uses [Worker]s to perform asynchronous work during the render pass by calling
 * [RenderContext.onWorkerOutput] or [RenderContext.runningWorker]. When equivalent [Worker]s are
 * passed in subsequent render passes, [doesSameWorkAs] is used to calculate which [Worker]s are
 * new and should be started, and which ones are continuations from the last render pass and
 * should be allowed to continue working. [Worker]s that are not included in a render pass are
 * cancelled.
 *
 * ## Example: Network request
 *
 * Let's say you have a network service with an API that returns a [Deferred], and you want to
 * call that service from a [Workflow].
 *
 * ```
 * interface TimeService {
 *   fun getTime(timezone: String): Deferred<Long>
 * }
 * ```
 *
 * The first step is to define a [Worker] that can call this service, and maybe an extension
 * function on your service class:
 * ```
 * fun TimeService.getTimeWorker(timezone: String): Worker<Long> = TimeWorker(timezone, this)
 *
 * private class TimeWorker(
 *   val timezone: String,
 *   val service: TimeService
 * ): Worker<Long> {
 *
 *   override suspend fun performWork(emitter: Emitter<Long>) {
 *     val timeDeferred = service.getTime(timezone)
 *     emitter.emitOutput(timeDeferred.await())
 *   }
 * }
 * ```
 *
 * You also need to define how to determine if a previous [Worker] is already doing the same work.
 * This will ensure that if the same request is made by the same [Workflow] in adjacent render
 * passes, we'll keep the request alive from the first pass.
 * ```
 *   override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
 *     otherWorker is TimeWorker &&
 *       timezone == otherWorker.timezone
 * ```
 *
 * Now you can request the time from your [Workflow]:
 * ```
 * class MyWorkflow(private val timeWorker: TimeWorker) {
 *   override fun render(…): Foo {
 *     context.onWorkerOutput(timeWorker) { time -> emitOutput("The time is $time") }
 *   }
 * ```
 *
 * Alternatively, if the response is a unique type, unlikely to be shared by any other workers,
 * you don't even need to create your own [Worker] class, you can use a builder, and the worker
 * will automatically be distinguished by that response type:
 * ```
 * interface TimeService {
 *   fun getTime(timezone: String): Deferred<TimeResponse>
 * }
 *
 * fun TimeService.getTimeWorker(timezone: String): Worker<TimeResponse> =
 *   Worker.from { getTime(timezone).await()) }
 * ```
 *
 * @see create
 * @see from
 * @see fromNullable
 * @see fromChannel
 * @see Deferred.asWorker
 * @see BroadcastChannel.asWorker
 */
interface Worker<out T> {

  /**
   * Passed into [performWork] to allow the [Worker] to emit outputs.
   */
  interface Emitter<in T> {
    /**
     * Delivers an output value to the [Workflow] that is running this [Worker].
     */
    suspend fun emitOutput(output: T)
  }

  /**
   * Used by [RenderContext.onWorkerOutputOrFinished] to distinguish between the two events of a
   * [Worker] emitting an output, and finishing (returning from [performWork]).
   */
  sealed class OutputOrFinished<out T> {
    data class Output<out T>(val value: T) : OutputOrFinished<T>()
    object Finished : OutputOrFinished<Nothing>()
  }

  /**
   * Override this method to do the actual work. This is a suspend function and is invoked in the
   * context of the workflow runtime. When this [Worker], it's parent [Workflow], or any ancestor
   * [Workflow]s are torn down, the coroutine in which this function is invoked will be cancelled.
   * If you need to do cleanup when the [Worker] is cancelled, wrap your work in a `try { }` block
   * and do your cleanup in a `finally { }` block. For more information on how coroutine
   * cancellation works, see [the coroutine guide](https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html).
   */
  suspend fun performWork(emitter: Emitter<T>)

  /**
   * Override this method to define equivalence between [Worker]s. At the end of every render pass,
   * the set of [Worker]s that were requested by the workflow are compared to the set from the last
   * render pass using this method. Equivalent workers are allowed to keep running. New workers
   * are started ([performWork] is called). Old workers are cancelled (by cancelling the scope in
   * which [performWork] is running).
   *
   * Implementations of this method should not be based on object identity. For example, a [Worker]
   * that performs a network request might check that two workers are requests to the same endpoint
   * and have the same request data.
   */
  fun doesSameWorkAs(otherWorker: Worker<*>): Boolean

  companion object {

    /**
     * Creates a [Worker] that will emit all values passed to `emit`, and then close when the block
     * returns.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     *
     * E.g.:
     * ```
     * val worker = Worker.create {
     *   emitOutput(1)
     *   delay(1000)
     *   emitOutput(2)
     *   delay(1000)
     *   emitOutput(3)
     * }
     * ```
     */
    inline fun <reified T> create(
      key: String = "",
      noinline block: suspend Emitter<T>.() -> Unit
    ): Worker<T> = TypedWorker(T::class, key, block)

    /**
     * Creates a [Worker] from a function that returns a single value.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    inline fun <reified T> from(
      key: String = "",
        // This could be crossinline, but there's a coroutines bug that will cause the coroutine
        // to immediately resume on suspension inside block when it is crossinline.
        // See https://youtrack.jetbrains.com/issue/KT-31197.
      noinline block: suspend () -> T
    ): Worker<T> = create(key) {
      emitOutput(block())
    }

    /**
     * Creates a [Worker] from a function that returns a single value.
     * The worker will emit the value **if and only if the value is not null**, then finish.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    inline fun <reified T : Any> fromNullable(
      key: String = "",
        // This could be crossinline, but there's a coroutines bug that will cause the coroutine
        // to immediately resume on suspension inside block when it is crossinline.
        // See https://youtrack.jetbrains.com/issue/KT-31197.
      noinline block: suspend () -> T?
    ): Worker<T> = create(key) {
      block()?.let { emitOutput(it) }
    }

    /**
     * Creates a [Worker] from a function that returns a [ReceiveChannel].
     * The worker will emit everything from the returned channel, and finish when the channel is
     * closed.
     *
     * If the [Worker] is torn down before finishing, both the scope and the channel will be
     * cancelled.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    inline fun <reified T> fromChannel(
      key: String = "",
      crossinline block: CoroutineScope.() -> ReceiveChannel<T>
    ): Worker<T> = create(key) {
      coroutineScope {
        val channel = block()
        // Close after cancel in case block doesn't use the passed CoroutineScope to scope the
        // returned channel.
        emitAll(channel, closeOnCancel = true)
      }
    }
  }
}

/**
 * Returns a [Worker] that will await this [Deferred] and then emit it.
 *
 * Note that [Deferred] is a "hot" future type – calling a function that _returns_ a [Deferred]
 * multiple times will probably perform the action multiple times. You may want to use something
 * like this instead:
 * ```
 * Worker.from { doThing().await() }
 * ```
 */
inline fun <reified T> Deferred<T>.asWorker(key: String = ""): Worker<T> = from(key) { await() }

/**
 * Returns a [Worker] that will, when performed,
 * [open a subscription][BroadcastChannel.openSubscription] to the channel and emit whatever it
 * receives. The subscription channel will be cancelled if the [Worker] is cancelled.
 */
@ExperimentalCoroutinesApi
inline fun <reified T> BroadcastChannel<T>.asWorker(key: String = ""): Worker<T> =
  fromChannel(key) { openSubscription() }

/**
 * Returns a [Worker] that will, when performed, emit whatever this channel receives.
 *
 * @param closeOnCancel
 * **If true:**
 * The channel _will_ be cancelled when the [Worker] is cancelled – this is intended for use with
 * cold channels that are were started by and are to be managed by this worker or its parent
 * [Workflow].
 *
 * **If false:**
 * The channel will _not_ be cancelled when the [Worker] is cancelled – this is intended for
 * use with hot channels that are managed externally.
 *
 * True by default.
 */
inline fun <reified T> ReceiveChannel<T>.asWorker(
  key: String = "",
  closeOnCancel: Boolean = true
): Worker<T> = create(key) {
  emitAll(this@asWorker, closeOnCancel)
}

/**
 * Emits whatever [channel] receives on this [Emitter].
 *
 * @param closeOnCancel
 * **If true:**
 * The channel _will_ be cancelled when the [Worker] is cancelled – this is intended for use with
 * cold channels that are were started by and are to be managed by this worker or its parent
 * [Workflow].
 *
 * **If false:**
 * The channel will _not_ be cancelled when the [Worker] is cancelled – this is intended for
 * use with hot channels that are managed externally.
 */
suspend inline fun <T> Emitter<T>.emitAll(
  channel: ReceiveChannel<T>,
  closeOnCancel: Boolean
) {
  if (closeOnCancel) {
    // Using consumeEach ensures that the channel is closed if this coroutine is cancelled.
    @Suppress("EXPERIMENTAL_API_USAGE")
    channel.consumeEach { emitOutput(it) }
  } else {
    for (value in channel) {
      emitOutput(value)
    }
  }
}

/**
 * A generic [Worker] implementation that defines equivalent workers as those having equivalent
 * [key]s and equivalent [type]s. This is used by all the [Worker] builder functions.
 */
@PublishedApi
internal class TypedWorker<T>(
  /** Can't be `KClass<T>` because `T` doesn't have upper bound `Any`. */
  private val type: KClass<*>,
  private val key: String,
  private val work: suspend Emitter<T>.() -> Unit
) : Worker<T> {

  override suspend fun performWork(emitter: Emitter<T>) = work(emitter)

  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is TypedWorker &&
        otherWorker.type == type &&
        otherWorker.key == key

  override fun toString(): String = "TypedWorker(key=\"$key\", type=$type)"
}
