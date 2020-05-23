package com.squareup.workflow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

private const val WORKER_COUNT = 1000

class WorkerStressTest {

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Test fun `multiple subscriptions to single channel when closed`() {
    val channel = Channel<Unit>()
    val workers = List(WORKER_COUNT / 2) { channel.asWorker() }
    val finishedWorkers = List(WORKER_COUNT / 2) {
      channel.asWorker()
          .transform { it.onCompletion { emit(Unit) } }
    }
    val action = action<Nothing, Unit> { setOutput(Unit) }
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      // Run lots of workers that will all see the same close event.
      workers.forEachIndexed { i, worker ->
        runningWorker(worker, key = i.toString()) {
          fail("Expected non-finishing worker $i not to emit.")
        }
      }
      finishedWorkers.forEachIndexed { i, worker ->
        runningWorker(worker, key = "finished $i") { action }
      }
    }

    runBlocking {
      val outputs = Channel<Unit>()
      renderWorkflowIn(workflow, this, MutableStateFlow(Unit)) {
        outputs.send(it)
      }

      // This should just work, and the test will finish, but this is broken by
      // https://github.com/Kotlin/kotlinx.coroutines/issues/1584 and will crash instead if
      // receiveOrClosed is used.
      channel.close()

      // Collect from all emitted workers to ensure they all reported their values.
      outputs.consumeAsFlow()
          .take(finishedWorkers.size)
          .collect()

      // Cancel the runtime so the test can finish.
      coroutineContext.cancelChildren()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Test fun `multiple subscriptions to single channel when emits`() {
    val channel = ConflatedBroadcastChannel(Unit)
    val workers = List(WORKER_COUNT) { channel.asWorker() }
    val action = action<Nothing, Int> { setOutput(1) }
    val workflow = Workflow.stateless<Unit, Int, Unit> {
      // Run lots of workers that will all see the same conflated channel value.
      workers.forEachIndexed { i, worker ->
        runningWorker(worker, key = i.toString()) { action }
      }
    }

    runBlocking {
      val outputs = Channel<Int>()
      renderWorkflowIn(workflow, this, MutableStateFlow(Unit)) {
        outputs.send(it)
      }
      val sum = outputs.consumeAsFlow()
          .take(workers.size)
          .reduce { sum, value -> sum + value }
      assertEquals(WORKER_COUNT, sum)

      // Cancel the runtime so the test can finish.
      coroutineContext.cancelChildren()
    }
  }
}
