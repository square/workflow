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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Represents a unit of asynchronous work that can have zero, one, or multiple outputs.
 *
 * Workers allow you to execute arbitrary, possibly asynchronous tasks in a declarative manner. To
 * perform their tasks, workers return a [Flow]. Workers are effectively [Flow]s that can be
 * [compared][doesSameWorkAs] to determine equivalence between render passes. A [Workflow] uses
 * Workers to perform asynchronous work during the render pass by calling
 * [RenderContext.runningWorker].
 *
 * See the documentation on [run] for more information on the returned [Flow] is consumed and how
 * to implement asynchronous work.
 *
 * See the documentation on [doesSameWorkAs] for more details on how and when workers are compared
 * and the worker lifecycle.
 *
 * ## Example: Network request
 *
 * Let's say you have a network service with an API that returns a number, and you want to
 * call that service from a [Workflow].
 *
 * ```
 * interface TimeService {
 *   suspend fun getTime(timezone: String): Long
 * }
 * ```
 *
 * The first step is to define a Worker that can call this service, and maybe an extension
 * function on your service class:
 * ```
 * fun TimeService.getTimeWorker(timezone: String): Worker<Long> = TimeWorker(timezone, this)
 *
 * private class TimeWorker(
 *   val timezone: String,
 *   val service: TimeService
 * ): Worker<Long> {
 *
 *   override fun run(): Flow<Long> = flow {
 *     val time = service.getTime(timezone)
 *     emit(time)
 *   }
 * }
 * ```
 *
 * You also need to define how to determine if a previous Worker is already doing the same work.
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
 * you don't even need to create your own Worker class, you can use a builder, and the worker
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
interface Worker<out OutputT> {

  /**
   * Returns a [Flow] to execute the work represented by this worker.
   *
   * [Flow] is "a cold asynchronous data stream that sequentially emits values and completes
   * normally or with an exception", although it may not emit any values. It is common to use
   * workers to perform some side effect that should only be executed when a state is entered – in
   * this case, the worker never emits anything (and will have type `Worker<Nothing>`).
   *
   * ## Coroutine Context
   *
   * When a worker is started, a coroutine is launched to [collect][Flow.collect] the flow.
   * When the worker is torn down, the coroutine is cancelled.
   * This coroutine is launched in the same scope as the workflow runtime, with a few changes:
   *
   * - The dispatcher is always set to [Unconfined][kotlinx.coroutines.Dispatchers.Unconfined] to
   *   minimize overhead for workers that don't care which thread they're executed on (e.g. logging
   *   side effects, workers that wrap third-party reactive libraries, etc.). If your work cares
   *   which thread it runs on, use [withContext][kotlinx.coroutines.withContext] or
   *   [flowOn][kotlinx.coroutines.flow.flowOn] to specify a dispatcher.
   * - A [CoroutineName][kotlinx.coroutines.CoroutineName] that describes the `Worker` instance
   *   (via `toString`) and the key specified by the workflow running the worker.
   *
   * ## Exceptions
   *
   * If a worker needs to report an error to the workflow running it, it *must not* throw it as an
   * exception – any exceptions thrown by a worker's [Flow] will cancel the entire workflow runtime.
   * Instead, the worker's [OutputT] type should be capable of expressing errors itself, and the
   * worker's logic should wrap any relevant exceptions into an output value (e.g. using the
   * [catch][kotlinx.coroutines.flow.catch] operator).
   *
   * While this might seem restrictive, this design decision keeps the [RenderContext.runningWorker]
   * API simpler, since it does not need to handle exceptions itself. It also discourages the code
   * smell of relying on exceptions to handle control flow.
   */
  fun run(): Flow<OutputT>

  /**
   * Override this method to define equivalence between [Worker]s.
   *
   * At the end of every render pass, the set of [Worker]s that were requested by the workflow are
   * compared to the set from the last render pass using this method. Equivalent workers are allowed
   * to keep running. New workers are started ([run] is called and the returned [Flow] is
   * collected). Old workers are cancelled by cancelling their collecting coroutines.
   *
   * Implementations of this method should not be based on object identity. For example, a [Worker]
   * that performs a network request might check that two workers are requests to the same endpoint
   * and have the same request data.
   *
   * Most implementations of this method will check for concrete type equality, and then match
   * on constructor parameters.
   *
   * E.g:
   *
   * ```
   * class SearchWorker(private val query: String): Worker<SearchResult> {
   *   // run omitted for example.
   *
   *   override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
   *     otherWorker is SearchWorker && otherWorker.query == query
   * }
   * ```
   */
  fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = otherWorker::class == this::class

  companion object {

    /**
     * Use this value instead of calling `typeOf<Nothing>()` directly, which isn't allowed because
     * [Nothing] isn't allowed as a reified type parameter.
     *
     * The KType of Nothing on the JVM is actually java.lang.Void if you do
     * Nothing::class.createType(). However createType() lives in the reflection library, so we just
     * reference Void directly so we don't have to add a dependency on kotlin-reflect.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private val TYPE_OF_NOTHING = typeOf<Void>()

    /**
     * Shorthand for `flow { block() }.asWorker()`.
     *
     * Note: If your worker just needs to perform side effects and doesn't need to emit anything,
     * use [createSideEffect] instead (since `Nothing` can't be used as a reified type parameter).
     */
    @OptIn(ExperimentalTypeInference::class)
    inline fun <reified OutputT> create(
      @BuilderInference noinline block: suspend FlowCollector<OutputT>.() -> Unit
    ): Worker<OutputT> = flow(block).asWorker()

    /**
     * Creates a [Worker] that just performs some side effects and doesn't emit anything. Run the
     * worker from your `render` method using [RenderContext.runningWorker].
     *
     * E.g.:
     * ```
     * fun logOnEntered(message: String) = Worker.createSideEffect() {
     *   println("Entered state: $message")
     * }
     *
     * Note that all workers created with this method are equivalent from the point of view of
     * their [Worker.doesSameWorkAs] methods. A workflow that needs multiple simultaneous
     * side effects can either bundle them all together into a single `createSideEffect`
     * call, or can use the `key` parameter to [RenderContext.runningWorker] to prevent conflicts.
     * ```
     */
    fun createSideEffect(
      block: suspend () -> Unit
    ): Worker<Nothing> = TypedWorker(TYPE_OF_NOTHING, flow { block() })

    /**
     * Returns a [Worker] that finishes immediately without emitting anything.
     */
    fun <T> finished(): Worker<T> = FinishedWorker

    /**
     * Creates a [Worker] from a function that returns a single value.
     *
     * Shorthand for `flow { emit(block()) }.asWorker()`.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    @OptIn(FlowPreview::class)
    inline fun <reified OutputT> from(noinline block: suspend () -> OutputT): Worker<OutputT> =
      block.asFlow().asWorker()

    /**
     * Creates a [Worker] from a function that returns a single value.
     * The worker will emit the value **if and only if the value is not null**, then finish.
     *
     * The returned [Worker] will equate to any other workers created with any of the [Worker]
     * builder functions that have the same output type.
     */
    inline fun <reified OutputT : Any> fromNullable(
        // This could be crossinline, but there's a coroutines bug that will cause the coroutine
        // to immediately resume on suspension inside block when it is crossinline.
        // See https://youtrack.jetbrains.com/issue/KT-31197.
      noinline block: suspend () -> OutputT?
    ): Worker<OutputT> = create {
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
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified OutputT> Flow<OutputT>.asWorker(): Worker<OutputT> =
  TypedWorker(typeOf<OutputT>(), this)

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
inline fun <reified OutputT> Deferred<OutputT>.asWorker(): Worker<OutputT> =
  from { await() }

/**
 * Shorthand for `.asFlow().asWorker()`.
 */
@OptIn(
    FlowPreview::class,
    ExperimentalCoroutinesApi::class
)
inline fun <reified OutputT> BroadcastChannel<OutputT>.asWorker(): Worker<OutputT> =
  asFlow().asWorker()

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
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified OutputT> ReceiveChannel<OutputT>.asWorker(
  closeOnCancel: Boolean = true
): Worker<OutputT> = create {
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
 * Returns a [Worker] that transforms this [Worker]'s [Flow] by calling [transform].
 *
 * The returned worker is considered equivalent with any other worker returned by this function
 * with the same receiver.
 *
 * ## Examples
 *
 * ### Workers from the same source are equivalent
 *
 * ```
 * val secondsWorker = millisWorker.transform {
 *   it.map { millis -> millis / 1000 }.distinctUntilChanged()
 * }
 *
 * val otherSecondsWorker = millisWorker.transform {
 *   it.map { millis -> millis.toSeconds() }
 * }
 *
 * assert(secondsWorker.doesSameWorkAs(otherSecondsWorker))
 * ```
 *
 * ### Workers from different sources are not equivalent
 *
 * ```
 * val secondsWorker = millisWorker.transform {
 *   it.map { millis -> millis / 1000 }.distinctUntilChanged()
 * }
 *
 * val otherSecondsWorker = secondsWorker.transform { it }
 *
 * assert(!secondsWorker.doesSameWorkAs(otherSecondsWorker))
 * ```
 */
fun <T, R> Worker<T>.transform(
  transform: (Flow<T>) -> Flow<R>
): Worker<R> = WorkerWrapper(
    wrapped = this,
    flow = transform(run())
)

/**
 * A generic [Worker] implementation that defines equivalent workers as those having equivalent
 * [type]s. This is used by all the [Worker] builder functions.
 */
@PublishedApi
internal class TypedWorker<OutputT>(
  private val type: KType,
  private val work: Flow<OutputT>
) : Worker<OutputT> {

  override fun run(): Flow<OutputT> = work

  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is TypedWorker && otherWorker.type == type

  override fun toString(): String = "TypedWorker($type)"
}

private class TimerWorker(
  private val delayMs: Long,
  private val key: String
) : Worker<Unit> {

  override fun run() = flow {
    delay(delayMs)
    emit(Unit)
  }

  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is TimerWorker && otherWorker.key == key

  override fun toString(): String = "TimerWorker(delayMs=$delayMs)"
}

private object FinishedWorker : Worker<Nothing> {
  override fun run(): Flow<Nothing> = emptyFlow()
  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = otherWorker === FinishedWorker
  override fun toString(): String = "FinishedWorker"
}

private class WorkerWrapper<T, R>(
  private val wrapped: Worker<T>,
  private val flow: Flow<R>
) : Worker<R> {
  override fun run(): Flow<R> = flow
  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is WorkerWrapper<*, *> &&
        wrapped.doesSameWorkAs(otherWorker.wrapped)

  override fun toString(): String = "WorkerWrapper($wrapped)"
}
