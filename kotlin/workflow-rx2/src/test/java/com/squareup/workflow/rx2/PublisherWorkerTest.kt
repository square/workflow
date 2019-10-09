package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.stateless
import com.squareup.workflow.testing.testFromStart
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.subjects.PublishSubject
import org.reactivestreams.Publisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PublisherWorkerTest {

  @Test fun works() {
    val subject = PublishSubject.create<String>()
    val worker = object : PublisherWorker<String>() {
      override fun runPublisher(): Publisher<out String> = subject.toFlowable(BUFFER)
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = otherWorker === this
    }

    fun action(value: String) = WorkflowAction<Nothing, String> { value }
    val workflow = Workflow.stateless<Unit, String, Unit> {
      runningWorker(worker) { action(it) }
    }

    workflow.testFromStart {
      assertFalse(hasOutput)

      subject.onNext("one")
      assertEquals("one", awaitNextOutput())

      subject.onNext("two")
      subject.onNext("three")
      assertEquals("two", awaitNextOutput())
      assertEquals("three", awaitNextOutput())
    }
  }
}
