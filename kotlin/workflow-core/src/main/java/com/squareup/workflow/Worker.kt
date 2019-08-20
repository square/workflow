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
import com.squareup.workflow.Worker.Companion.fromNullable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlin.experimental.ExperimentalTypeInference
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
 * @see Deferred.asWorker
 * @see BroadcastChannel.asWorker
 */
interface Worker<out T> {

  /**
   * Used by [RenderContext.runningWorkerUntilFinished] to distinguish between the two events of a
   * [Worker] emitting an output and finishing.
   */
  sealed class OutputOrFinished<out T> {
    /**
     * Indicates that a [Worker] emitted an output value.
     */
    data class Output<out T>(val value: T) : OutputOrFinished<T>()

    /**
     * Indicates that a [Worker] finished, and will not emit any more output values.
     */
    object Finished : OutputOrFinished<Nothing>() {
      override fun toString(): String = "Finished"
    }
  }

  /**
   * Returns a [Flow] to execute the work represented by this worker.
   *
   * The [Flow] is invoked in the context of the workflow runtime. When this [Worker], its parent
   * [Workflow], or any ancestor [Workflow]s are torn down, the coroutine in which this [Flow] is
   * being collected will be cancelled.
   */
  @UseExperimental(ExperimentalCoroutinesApi::class)
  fun run(): Flow<T>

  /**
   * Override this method to define equivalence between [Worker]s. At the end of every render pass,
   * the set of [Worker]s that were requested by the workflow are compared to the set from the last
   * render pass using this method. Equivalent workers are allowed to keep running. New workers
   * are started ([run] is called and the returned [Flow] is collected). Old workers are cancelled
   * by cancelling their collecting coroutines.
   *
   * Implementations of this method should not be based on object identity. For example, a [Worker]
   * that performs a network request might check that two workers are requests to the same endpoint
   * and have the same request data.
   */
  fun doesSameWorkAs(otherWorker: Worker<*>): Boolean

  companion object {

    /**
     * Shorthand for `flow { block() }.asWorker(key)`.
     *
     * Note: If your worker just needs to perform side effects and doesn't need to emit anything,
     * use [createSideEffect] instead (since `Nothing` can't be used as a reified type parameter).
     */
    @UseExperimental(ExperimentalTypeInference::class, ExperimentalCoroutinesApi::class)
    inline fun <reified T> create(
      key: String = "",
      @BuilderInference noinline block: suspend FlowCollector<T>.() -> Unit
    ): Worker<T> = flow(block).asWorker(key)

    /**
     * Creates a [Worker] that just performs some side effects and doesn't emit anything. Run the
     * worker from your `render` method using [RenderContext.runningWorker].
     *
     * The returned [Worker] will equate to any other workers created with this function that have
     * the same key. The key is required for this builder because there is no type information
     * available to distinguish workers.
     *
     * E.g.:
     * ```
     * fun logOnEntered(message: String) = Worker.createSideEffect("logOnEntered") {
     *   println("Entered state: $message")
     * }
     * ```
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun createSideEffect(
      key: String,
      block: suspend () -> Unit
    ): Worker<Nothing> = TypedWorker(Nothing::class, key, flow { block() })

    /**
     * Creates a [Worker] from a function that returns a single value.
     *
     * Shorthand for `flow { emit(block()) }.asWorker(key)`.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    @UseExperimental(FlowPreview::class, ExperimentalCoroutinesApi::class)
    inline fun <reified T> from(
      key: String = "",
      noinline block: suspend () -> T
    ): Worker<T> = block.asFlow().asWorker(key)

    /**
     * Creates a [Worker] from a function that returns a single value.
     * The worker will emit the value **if and only if the value is not null**, then finish.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    inline fun <reified T : Any> fromNullable(
      key: String = "",
        // This could be crossinline, but there's a coroutines bug that will cause the coroutine
        // to immediately resume on suspension inside block when it is crossinline.
        // See https://youtrack.jetbrains.com/issue/KT-31197.
      noinline block: suspend () -> T?
    ): Worker<T> = create(key) {
      block()?.let { emit(it) }
    }

    /**
     * Creates a [Worker] that will emit [Unit] and then finish after [delayMs] milliseconds.
     * Negative delays are clamped to zero.
     *
     * Workers returned by this function will be compared by [key].
     */
    fun timer(
      delayMs: Long,
      key: String = ""
    ): Worker<Unit> = TimerWorker(delayMs, key)
  }
}

/**
 * Returns a [Worker] that will, when performed, emit whatever this [Flow] receives.
 *
 * **Warning:** The Flow API is very immature and so any breaking changes there (including in
 * transiently-included versions) will be compounded when layering Workflow APIs on top of it.
 * This **SHOULD NOT** be used in production code.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
inline fun <reified T> Flow<T>.asWorker(
  key: String = ""
): Worker<T> = TypedWorker(T::class, key, this)

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
 * Shorthand for `.asFlow().asWorker(key)`.
 */
@ExperimentalCoroutinesApi
@UseExperimental(FlowPreview::class)
inline fun <reified T> BroadcastChannel<T>.asWorker(key: String = ""): Worker<T> =
  asFlow().asWorker(key)

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
@UseExperimental(ExperimentalCoroutinesApi::class)
inline fun <reified T> ReceiveChannel<T>.asWorker(
  key: String = "",
  closeOnCancel: Boolean = true
): Worker<T> = create(key) {
  if (closeOnCancel) {
    // Using consumeEach ensures that the channel is closed if this coroutine is cancelled.
    consumeEach { emit(it) }
  } else {
    for (value in this@asWorker) {
      emit(value)
    }
  }
}

/**
 * A generic [Worker] implementation that defines equivalent workers as those having equivalent
 * [key]s and equivalent [type]s. This is used by all the [Worker] builder functions.
 */
@PublishedApi
@UseExperimental(ExperimentalCoroutinesApi::class)
internal class TypedWorker<T>(
  /** Can't be `KClass<T>` because `T` doesn't have upper bound `Any`. */
  private val type: KClass<*>,
  private val key: String,
  private val work: Flow<T>
) : Worker<T> {

  override fun run(): Flow<T> = work

  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is TypedWorker &&
        otherWorker.type == type &&
        otherWorker.key == key

  override fun toString(): String = "TypedWorker(key=\"$key\", type=$type)"
}

private class TimerWorker(
  private val delayMs: Long,
  private val key: String
) : Worker<Unit> {

  @UseExperimental(ExperimentalCoroutinesApi::class)
  override fun run() = flow {
    delay(delayMs)
    emit(Unit)
  }

  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is TimerWorker && otherWorker.key == key
}
