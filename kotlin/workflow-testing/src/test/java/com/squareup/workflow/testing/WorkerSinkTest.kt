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
package com.squareup.workflow.testing

import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerSinkTest {

  @Test fun `workers are equivalent with matching type and name`() {
    val fooWorker = WorkerSink<String>("foo")
    val otherFooWorker = WorkerSink<String>("foo")
    assertTrue(fooWorker.doesSameWorkAs(otherFooWorker))
  }

  @Test fun `workers are not equivalent with differing names`() {
    val fooWorker = WorkerSink<String>("foo")
    val barWorker = WorkerSink<String>("bar")
    assertFalse(fooWorker.doesSameWorkAs(barWorker))
  }

  @Test fun `workers are not equivalent with differing types`() {
    val stringWorker = WorkerSink<String>("foo")
    val intWorker = WorkerSink<Int>("foo")
    assertFalse(stringWorker.doesSameWorkAs(intWorker))
  }

  @Test fun `values sent before running are received`() {
    val worker = WorkerSink<String>("foo")
    worker.send("hello")
    runBlocking {
      assertEquals("hello", worker.run().first())
    }
  }

  @Test fun `values sent after running are received`() {
    val worker = WorkerSink<String>("foo")
    runBlocking {
      val deferred = async {
        worker.run()
            .first()
      }
      yield()
      worker.send("hello")
      assertEquals("hello", deferred.await())
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test fun `multiple values are buffered`() {
    val worker = WorkerSink<String>("foo")
    worker.send("hello")
    worker.send("world")
    worker.send("goodbye")
    runBlocking {
      assertEquals(listOf("hello", "world", "goodbye"), worker.run().take(3).toList())
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test fun `throws when consumed concurrently`() {
    val worker = WorkerSink<String>("foo")

    runBlocking {
      launch(start = UNDISPATCHED) {
        worker.run()
            .collect()
      }

      assertFailsWith<IllegalStateException> {
        worker.run()
            .collect()
      }

      coroutineContext.cancelChildren()
    }
  }

  @Test fun `can be consumed multiple times sequentially`() {
    val worker = WorkerSink<String>("foo")
    worker.send("foo")
    worker.send("bar")

    runBlocking {
      assertEquals("foo", worker.run().first())
      assertEquals("bar", worker.run().first())
    }
  }
}
