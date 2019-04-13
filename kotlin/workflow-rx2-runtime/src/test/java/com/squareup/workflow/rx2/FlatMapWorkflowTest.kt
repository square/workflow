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

import com.squareup.workflow.Workflow
import com.squareup.workflow.stateless
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import kotlin.test.Test

class FlatMapWorkflowTest {

  private val workflow = Workflow.stateless<String, Nothing, String> { input, _ ->
    "rendered: $input"
  }
  private val inputs = PublishSubject.create<String>()
  private val renderings: TestSubscriber<String> =
    inputs.toFlowable(BUFFER)
        .flatMapWorkflow(workflow)
        .map { it.rendering }
        .test()

  @Test fun `doesn't emit until input emitted`() {
    renderings.assertNoValues()
    renderings.assertNotTerminated()
  }

  @Test fun `single input`() {
    inputs.onNext("input")

    renderings.assertValue("rendered: input")
    renderings.assertNotTerminated()
  }

  @Test fun `multiple inputs`() {
    inputs.onNext("one")
    inputs.onNext("two")
    inputs.onNext("three")
    renderings.assertValues("rendered: one", "rendered: two", "rendered: three")
    renderings.assertNotTerminated()
  }

  @Test fun `output doesn't complete after input completes`() {
    inputs.onNext("input")
    inputs.onComplete()
    renderings.assertNotTerminated()
  }

  @Test fun `output errors when input completes before emitting`() {
    inputs.onComplete()
    renderings.assertError { it is NoSuchElementException }
  }
}
