package com.squareup.workflow

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CompletableDeferred
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerTest {

  private val pool = WorkflowPool()
  private val deferred = CompletableDeferred<Unit>()
  private val worker = deferred.asWorker()

  @Test fun whenCallSucceeds() {
    val reaction = pool.workerResult(worker, Unit)
    assertFalse(reaction.isCompleted)

    deferred.complete(Unit)

    assertEquals(Unit, reaction.getCompleted())
  }

  @Test fun whenCallFails() {
    val reaction = pool.workerResult(worker, Unit)
    assertFalse(reaction.isCompleted)

    deferred.completeExceptionally(IOException("network fail"))

    val failure = reaction.getCompletionExceptionOrNull()!!
    assertTrue(failure is ReactorException)
    assertTrue(failure.cause is IOException)
  }

  @Test fun whenInternalCoroutineCancelled() {
    val reaction = pool.workerResult(worker, Unit)
    assertFalse(reaction.isCompleted)

    deferred.cancel()

    assertFailsWith<CancellationException> { reaction.getCompleted() }
  }

  @Test fun whenWorkflowCancelled() {
    val reaction = pool.workerResult(worker, Unit)
    assertFalse(reaction.isCompleted)

    pool.abandonDelegate(worker.makeId())

    assertFailsWith<CancellationException> { reaction.getCompleted() }
  }
}
