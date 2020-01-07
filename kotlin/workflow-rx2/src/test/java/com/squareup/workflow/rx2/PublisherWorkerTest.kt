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
package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
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

    fun action(value: String) = action<Nothing, String> { setOutput(value) }
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
