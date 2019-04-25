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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class WorkerTest {

  private class ExpectedException : RuntimeException()

  @Test fun `create returns equivalent workers unkeyed`() {
    val worker1 = Worker.create<Unit> {}
    val worker2 = Worker.create<Unit> {}

    assertNotSame(worker1, worker2)
    assertTrue(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `create returns equivalent workers keyed`() {
    val worker1 = Worker.create<Unit>("key") {}
    val worker2 = Worker.create<Unit>("key") {}

    assertNotSame(worker1, worker2)
    assertTrue(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `create returns non-equivalent workers based on type`() {
    val worker1 = Worker.create<Unit> {}
    val worker2 = Worker.create<Int> {}

    assertFalse(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `create returns non-equivalent workers based on key`() {
    val worker1 = Worker.create<Unit>("key1") {}
    val worker2 = Worker.create<Unit>("key2") {}

    assertFalse(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `create emits and finishes`() {
    val worker = Worker.create<String> {
      emitOutput("hello")
      emitOutput("world")
    }

    worker.test {
      assertEquals("hello", nextOutput())
      assertEquals("world", nextOutput())
      assertFinished()
    }
  }

  @Test fun `create finishes without emitting`() {
    val worker = Worker.create<String> {}

    worker.test {
      assertFinished()
    }
  }

  @Test fun `create propagates exceptions`() {
    val worker = Worker.create<Unit> { throw ExpectedException() }

    worker.test {
      assertTrue(getException() is ExpectedException)
    }
  }

  @Test fun `from emits and finishes`() {
    val worker = Worker.from { "foo" }

    worker.test {
      assertEquals("foo", nextOutput())
      assertFinished()
    }
  }

  @Test fun `from emits null`() {
    val worker = Worker.from<String?> { null }

    worker.test {
      assertEquals(null, nextOutput())
      assertFinished()
    }
  }

  @Test fun `fromNullable emits and finishes`() {
    val worker = Worker.fromNullable { "foo" }

    worker.test {
      assertEquals("foo", nextOutput())
      assertFinished()
    }
  }

  @Test fun `fromNullable doesn't emit null`() {
    val worker = Worker.fromNullable<String> { null }

    worker.test {
      assertFinished()
    }
  }

  @Test fun `fromChannel emits from channel then finishes`() {
    val worker = Worker.fromChannel {
      produce {
        send("hello")
        send("world")
      }
    }

    worker.test {
      assertEquals("hello", nextOutput())
      assertEquals("world", nextOutput())
      assertFinished()
    }
  }

  @Test fun `fromChannel finishes without emitting`() {
    val worker = Worker.fromChannel {
      Channel<Unit>().apply { close() }
    }

    worker.test {
      assertFinished()
    }
  }

  @Test fun `fromChannel cancels scope when worker cancelled`() {
    var cancelled = false
    val worker = Worker.fromChannel {
      coroutineContext[Job]!!.invokeOnCompletion {
        assertNotNull(it)
        cancelled = true
      }
      Channel<Unit>()
    }

    worker.test {
      cancelWorker()
      assertTrue(cancelled)
    }
  }

  @Test fun `fromChannel cancels channel when worker cancelled`() {
    var cancelled = false
    val worker = Worker.fromChannel {
      Channel<Unit>().apply {
        invokeOnClose {
          cancelled = true
        }
      }
    }

    worker.test {
      cancelWorker()
      assertTrue(cancelled)
    }
  }

  @Test fun `fromChannel propagates exceptions`() {
    val worker = Worker.fromChannel {
      Channel<Unit>().apply {
        // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
        @Suppress("DEPRECATION")
        cancel(ExpectedException())
      }
    }

    worker.test {
      assertTrue(getException() is ExpectedException)
    }
  }

  @Test fun `ReceiveChannel asWorker emits`() {
    val channel = Channel<String>(capacity = 1)
    val worker = channel.asWorker()

    worker.test {
      channel.send("hello")
      assertEquals("hello", nextOutput())

      channel.send("world")
      assertEquals("world", nextOutput())
    }
  }

  @Test fun `ReceiveChannel asWorker finishes`() {
    val channel = Channel<String>()
    val worker = channel.asWorker()

    worker.test {
      channel.close()
      assertFinished()
    }
  }

  @Test fun `ReceiveChannel asWorker does close channel when cancelled`() {
    val channel = Channel<Unit>(capacity = 1)
    val worker = channel.asWorker()

    worker.test {
      cancelWorker()
      assertTrue(channel.isClosedForReceive)
    }
  }

  @Test
  fun `ReceiveChannel asWorker doesn't close channel when cancelled when closeOnCancel false`() {
    val channel = Channel<Unit>(capacity = 1)
    val worker = channel.asWorker(closeOnCancel = false)

    worker.test {
      cancelWorker()
      channel.send(Unit)
      assertEquals(Unit, channel.receive())
    }
  }
}
