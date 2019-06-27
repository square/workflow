package com.squareup.workflow

import com.squareup.workflow.WorkflowHost.RenderingAndSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.ATOMIC
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Implements the [WorkflowHost] interface by [launching][CoroutineScope.launch] a new coroutine
 * on [start].
 *
 * @param context The context used to launch the runtime coroutine.
 * @param run Gets invoked from a new coroutine started with `context`, and passed functions
 * that will cause [renderingsAndSnapshots] and [outputs] to emit, respectively. If any exception
 * is thrown, those Flows will both rethrow the exception.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
internal class RealWorkflowHost<O : Any, R>(
  context: CoroutineContext,
  private val run: suspend (
    onRendering: suspend (RenderingAndSnapshot<R>) -> Unit,
    onOutput: suspend (O) -> Unit
  ) -> Unit
) : WorkflowHost<O, R> {

  /**
   * This must be a [SupervisorJob] because if any of the workflow coroutines fail, we will handle
   * the error explicitly by using it to close the [renderingsAndSnapshots] and [outputs] streams.
   */
  private val scope = CoroutineScope(context + SupervisorJob(parent = context[Job]))

  private val _renderingsAndSnapshots = ConflatedBroadcastChannel<RenderingAndSnapshot<R>>()
  private val _outputs = BroadcastChannel<O>(capacity = 1)

  private var job: Job? = null

  @UseExperimental(FlowPreview::class)
  override val renderingsAndSnapshots: Flow<RenderingAndSnapshot<R>>
    get() = _renderingsAndSnapshots.asFlow()

  @UseExperimental(FlowPreview::class)
  override val outputs: Flow<O>
    get() = _outputs.asFlow()

  override fun start(): Job {
    return job ?: scope
        // We need to launch atomically so that, if scope is already cancelled, the coroutine is
        // still allowed to handle the error by closing the channels.
        .launch(start = ATOMIC) {
          val result = runCatching {
            run(_renderingsAndSnapshots::send, _outputs::send)
          }
          // We need to unwrap the cancellation exception so that we *complete* the channels instead
          // of cancelling them if our coroutine was merely cancelled.
          val error = result.exceptionOrNull()
              ?.unwrapCancellationCause()
          _renderingsAndSnapshots.close(error)
          _outputs.close(error)
          result.getOrThrow()
        }
        .also { job = it }
  }
}

private tailrec fun Throwable.unwrapCancellationCause(): Throwable? {
  if (this !is CancellationException) return this
  return cause?.unwrapCancellationCause()
}
