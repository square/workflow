package com.squareup.workflow

import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Type
import kotlinx.coroutines.experimental.Deferred

/**
 * Defines a discrete task that a [Workflow] can execute asynchronously via [WorkflowPool],
 * possibly across state changes.
 *
 * # Defining Workers
 *
 * There are a few ways to define a worker:
 *  - Call [worker] and pass a suspending block:
 *    ```
 *    val echoWorker = worker { input ->
 *      withContext(IO) {
 *        echoService.echo(input)
 *      }
 *    }
 *    ```
 *
 *  - Convert a [Deferred] to a worker directly:
 *    ```
 *    val deferredWorker = deferred.asWorker()
 *    ```
 *
 *  - Implement the `Worker` interface and implement [call]:
 *    ```
 *    class EchoWorker(
 *      private val echoService: EchoService,
 *      private val ioContext: CoroutineContext
 *    ) : Worker<String, String> {
 *      override suspend fun call(input: String): String =
 *        withContext(ioContext) {
 *          echoService.echo(input)
 *        }
 *    }
 *    ```
 *
 * # Running Workers
 *
 * To use, pass an instance of your [Worker] to [WorkflowPool.awaitWorkerResult] along with its
 * input value. You may also pass an optional `name` `String` if you need to distinguish between
 * multiple invocations. E.g.:
 *
 * ```
 * class MainReactor(private val echo: EchoWorker) : Reactor<…> {
 *   override suspend fun onReact(
 *     …,
 *     workflows: WorkflowPool
 *   ): Reaction<…, …> {
 *     …
 *     workflows.awaitWorkerResult(echo, "hello world")
 *       .let { result: String ->
 *         // React to result.
 *       }
 *   }
 * }
 * ```
 */
interface Worker<in I : Any, out O : Any> {
  /**
   * Perform some asynchronous work.
   */
  suspend fun call(input: I): O
}

/**
 * Creates a [Worker] that passes [block] its input value and uses its return value as the result.
 */
fun <I : Any, O : Any> worker(block: suspend (I) -> O): Worker<I, O> = object : Worker<I, O> {
  override suspend fun call(input: I): O = block(input)
}

/**
 * Creates a [Worker] that will report the [Deferred]'s eventual value as its result.
 */
fun <T : Any> Deferred<T>.asWorker(): Worker<Unit, T> = worker { await() }

/**
 * Uniquely identifies the [Worker] across the [WorkflowPool].
 * See [WorkflowPool.Type.makeId] for details.
 */
@Suppress("unused")
inline fun <reified I : Any, reified O : Any> Worker<I, O>.makeId(
  name: String = ""
): Id<I, Nothing, O> = workflowType.makeId(name)

/**
 * Returns the [Type] of the [Worker] for the [WorkflowPool]
 */
@PublishedApi
@Suppress("unused")
internal inline val <reified I : Any, reified O : Any>
    Worker<I, O>.workflowType: Type<I, Nothing, O>
  get() = Type(I::class, Nothing::class, O::class)
