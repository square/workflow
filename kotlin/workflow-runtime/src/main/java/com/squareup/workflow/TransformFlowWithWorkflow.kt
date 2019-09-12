package com.squareup.workflow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.produceIn
import kotlin.experimental.ExperimentalTypeInference

/**
 * TODO write documentation
 */
@UseExperimental(
    ExperimentalCoroutinesApi::class,
    ExperimentalTypeInference::class,
    FlowPreview::class
)
fun <T, R : Any> Flow<T>.transformWithWorkflow(workflow: Workflow<Worker<T>, R, Unit>): Flow<R> =
  flow {
    coroutineScope {
      val scope = this
      val upstreamWorker = UpstreamWorker(this@transformWithWorkflow)
      val props = flowOf(upstreamWorker)
      val outputChannel = launchWorkflowIn(scope, workflow, props) { _, outputs ->
        outputs.produceIn(scope)
      }
      emitAll(outputChannel)
    }
  }

private class UpstreamWorker<T>(private val upstream: Flow<T>) : Worker<T> {
  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    otherWorker is UpstreamWorker && otherWorker.upstream === upstream

  override fun run(): Flow<T> = upstream
}
