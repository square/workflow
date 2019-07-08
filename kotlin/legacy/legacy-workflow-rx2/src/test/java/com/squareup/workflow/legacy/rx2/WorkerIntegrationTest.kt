/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.legacy.Worker
import com.squareup.workflow.legacy.WorkflowPool
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.SingleSubject
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import java.io.IOException

class WorkerIntegrationTest {

  private val pool = WorkflowPool()
  private val single = SingleSubject.create<String>()
  private val worker = single.asWorker()

  @Test fun `when call succeeds`() {
    val reaction = pool.workerResult(worker, Unit)
    reaction.assertNotTerminated()

    single.onSuccess("bar")

    reaction.assertValue("bar")
  }

  @Test fun `when call fails`() {
    val reaction = pool.workerResult(worker, Unit)
    reaction.assertNotTerminated()

    single.onError(IOException("network fail"))

    val failure = reaction.errors()
        .single()
    assertThat(failure).isInstanceOf(IOException::class.java)
  }

  @Test fun `when worker cancelled`() {
    val reaction = pool.workerResult(worker, Unit)
    reaction.assertNotTerminated()

    pool.abandonWorker(worker)

    // The rx2 version of nextProcessResult will never complete the single if the workflow is
    // cancelled.
    reaction.assertNoValues()
    reaction.assertNoErrors()
  }
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified I : Any, reified O : Any> WorkflowPool.workerResult(
  worker: Worker<I, O>,
  input: I
): TestObserver<O> = Channel<Nothing>().asEventChannel()
    .select<O> { onWorkerResult(worker, input) { it } }
    .test() as TestObserver<O>
