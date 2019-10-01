package com.squareup.workflow

import com.squareup.workflow.WorkflowAction.Companion.noAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkerStressTest {

  @UseExperimental(ExperimentalCoroutinesApi::class)
  @Test fun `multiple subscriptions to single channel when closed`() {
    val channel = Channel<Unit>()
    val workers = List(100) { channel.asWorker() }
    val finishedWorkers = List(100) {
      channel.asWorker()
          .transform { it.onCompletion { emit(Unit) } }
    }
    val action = WorkflowAction<Nothing, Unit> { Unit }
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      // Run lots of workers that will all see the same close event.
      workers.forEachIndexed { i, worker ->
        runningWorker(worker, key = i.toString()) { noAction() }
      }
      finishedWorkers.forEachIndexed { i, worker ->
        runningWorker(worker, key = "finished $i") { action }
      }
    }

    runBlocking {
      val outputs = launchWorkflowIn(this, workflow, flowOf(Unit)) { it.outputs }

      // This should just work, and the test will finish, but this is broken by
      // https://github.com/Kotlin/kotlinx.coroutines/issues/1584 and will crash instead if
      // receiveOrClosed is used.
      channel.close()
      delay(1)

      outputs.take(100)
          .collect()

      // Cancel the runtime so the test can finish.
      coroutineContext.cancelChildren()
    }
  }

  @UseExperimental(ExperimentalCoroutinesApi::class)
  @Test fun `multiple subscriptions to single channel when emits`() {
    val channel = ConflatedBroadcastChannel(Unit)
    val workers = List(100) { channel.asWorker() }
    val action = WorkflowAction<Nothing, Int> { 1 }
    val workflow = Workflow.stateless<Unit, Int, Unit> {
      // Run lots of workers that will all see the same conflated channel value.
      workers.forEachIndexed { i, worker ->
        runningWorker(worker, key = i.toString()) { action }
      }
    }

    runBlocking {
      val outputs = launchWorkflowIn(this, workflow, flowOf(Unit)) { it.outputs }
      val sum = outputs.take(100)
          .reduce { sum, value -> sum + value }
      assertEquals(100, sum)

      // Cancel the runtime so the test can finish.
      coroutineContext.cancelChildren()
    }
  }
}
