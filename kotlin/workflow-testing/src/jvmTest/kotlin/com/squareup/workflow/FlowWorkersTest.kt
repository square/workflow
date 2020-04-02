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

package com.squareup.workflow

import com.squareup.workflow.testing.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowWorkersTest {

  private class ExpectedException : RuntimeException()

  private val subject = Channel<String>(capacity = 1)
  private var source = flow { subject.consumeEach { emit(it) } }

  private val worker by lazy { source.asWorker() }

  @Test fun `flow emits`() {
    worker.test {
      subject.send("foo")
      assertEquals("foo", nextOutput())

      subject.send("bar")
      assertEquals("bar", nextOutput())
    }
  }

  @Test fun `flow finishes`() {
    worker.test {
      subject.close()
      assertFinished()
    }
  }

  @Test fun `flow finishes after emitting interleaved`() {
    worker.test {
      subject.send("foo")
      assertEquals("foo", nextOutput())

      subject.close()
      assertFinished()
    }
  }

  @Test fun `flow finishes after emitting grouped`() {
    worker.test {
      subject.send("foo")
      subject.close()

      assertEquals("foo", nextOutput())
      assertFinished()
    }
  }

  @Test fun `flow throws`() {
    worker.test {
      subject.close(ExpectedException())
      assertTrue(getException() is ExpectedException)
    }
  }

  @Test fun `flow is collected lazily`() {
    var collections = 0
    source = source.onCollect { collections++ }

    assertEquals(0, collections)

    worker.test {
      assertEquals(1, collections)
    }
  }

  @Test fun `flow is cancelled when worker cancelled`() {
    var cancellations = 0
    source = source.onCancel { cancellations++ }

    assertEquals(0, cancellations)

    worker.test {
      assertEquals(0, cancellations)
      cancelWorker()
      assertEquals(1, cancellations)
    }
  }

  private fun <T> Flow<T>.onCollect(action: suspend () -> Unit) = flow {
    action()
    collect { emit(it) }
  }

  private fun <T> Flow<T>.onCancel(action: suspend () -> Unit) = flow {
    try {
      collect { emit(it) }
    } catch (e: CancellationException) {
      action()
      throw e
    }
  }
}
