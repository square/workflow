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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow.internal

import com.squareup.workflow.asWorker
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkersTest {

  @Test fun `propagates backpressure`() {
    val channel = Channel<String>()
    val worker = channel.asWorker()
    // Used to assert ordering.
    val counter = AtomicInteger(0)

    runBlocking {
      val workerOutputs = launchWorker(worker)

      launch(start = UNDISPATCHED) {
        assertEquals(0, counter.getAndIncrement())
        channel.send("a")
        assertEquals(2, counter.getAndIncrement())
        channel.send("b")
        assertEquals(4, counter.getAndIncrement())
        channel.close()
        assertEquals(5, counter.getAndIncrement())
      }
      yield()
      assertEquals(1, counter.getAndIncrement())

      assertEquals("a", workerOutputs.poll())
      yield()
      assertEquals(3, counter.getAndIncrement())

      assertEquals("b", workerOutputs.poll())
      yield()
      assertEquals(6, counter.getAndIncrement())

      // Cancel the worker so we can exit this loop.
      workerOutputs.cancel()
    }
  }
}
