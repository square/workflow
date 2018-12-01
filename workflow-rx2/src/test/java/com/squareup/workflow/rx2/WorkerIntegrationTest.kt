package com.squareup.workflow.rx2

import com.squareup.workflow.ReactorException
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.makeId
import io.reactivex.subjects.SingleSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import java.io.IOException

class WorkerIntegrationTest {

  private val pool = WorkflowPool()
  private val single = SingleSubject.create<Unit>()
  private val worker = single.asWorker()

  @Test fun whenCallSucceeds() {
    val reaction = pool.workerResult(worker, Unit)
        .test()
    reaction.assertNotTerminated()

    single.onSuccess(Unit)

    reaction.assertValue(Unit)
  }

  @Test fun whenCallFails() {
    val reaction = pool.workerResult(worker, Unit)
        .test()
    reaction.assertNotTerminated()

    single.onError(IOException("network fail"))

    val failure = reaction.errors()
        .single()
    assertThat(failure).isInstanceOf(ReactorException::class.java)
    assertThat(failure.cause is IOException).isTrue()
  }

  @Test fun whenWorkflowCancelled() {
    val reaction = pool.workerResult(worker, Unit)
        .test()
    reaction.assertNotTerminated()

    pool.abandonDelegate(worker.makeId())

    // The rx2 version of nextProcessResult will never complete the single if the workflow is
    // cancelled.
    reaction.assertNoValues()
    reaction.assertNoErrors()
  }
}
