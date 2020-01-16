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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class WorkerTest {

  private class ExpectedException : RuntimeException()

  @Test fun `create returns equivalent workers`() {
    val worker1 = Worker.create<Unit> {}
    val worker2 = Worker.create<Unit> {}

    assertNotSame(worker1, worker2)
    assertTrue(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `create returns non-equivalent workers based on type`() {
    val worker1 = Worker.create<Unit> {}
    val worker2 = Worker.create<Int> {}

    assertFalse(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `create emits and finishes`() {
    val worker = Worker.create {
      emit("hello")
      emit("world")
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

  @Test fun `createSideEffect returns equivalent workers`() {
    val worker1 = Worker.createSideEffect {}
    val worker2 = Worker.createSideEffect {}

    assertNotSame(worker1, worker2)
    assertTrue(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `createSideEffect runs`() {
    var ran = false
    val worker = Worker.createSideEffect {
      ran = true
    }

    worker.test {
      assertTrue(ran)
    }
  }

  @Test fun `createSideEffect finishes`() {
    val worker = Worker.createSideEffect {}

    worker.test {
      assertFinished()
    }
  }

  @Test fun `createSideEffect propagates exceptions`() {
    val worker = Worker.createSideEffect { throw ExpectedException() }

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

  @Test fun `timer returns equivalent workers keyed`() {
    val worker1 = Worker.timer(1, "key")
    val worker2 = Worker.timer(1, "key")

    assertNotSame(worker1, worker2)
    assertTrue(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `timer returns non-equivalent workers based on key`() {
    val worker1 = Worker.timer(1, "key1")
    val worker2 = Worker.timer(1, "key2")

    assertFalse(worker1.doesSameWorkAs(worker2))
  }

  @Test fun `timer emits and finishes after delay`() {
    val testDispatcher = TestCoroutineDispatcher()
    val worker = Worker.timer(1000)
        // Run the timer on the test dispatcher so we can control time.
        .transform { it.flowOn(testDispatcher) }

    worker.test {
      assertNoOutput()
      assertNotFinished()

      testDispatcher.advanceTimeBy(999)
      assertNoOutput()
      assertNotFinished()

      testDispatcher.advanceTimeBy(1)
      assertEquals(Unit, nextOutput())
      assertFinished()
    }
  }

  @Test fun `finished worker is equivalent to self`() {
    assertTrue(Worker.finished<Nothing>().doesSameWorkAs(Worker.finished<Nothing>()))
  }

  @Test fun `transformed workers are equivalent with equivalent source`() {
    val source = Worker.create<Unit> {}
    val transformed1 = source.transform { flow -> flow.buffer(1) }
    val transformed2 = source.transform { flow -> flow.conflate() }

    assertTrue(transformed1.doesSameWorkAs(transformed2))
  }

  @Test fun `transformed workers are not equivalent with nonequivalent source`() {
    val source1 = object : Worker<Unit> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = false
      override fun run(): Flow<Unit> = emptyFlow()
    }
    val source2 = object : Worker<Unit> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = false
      override fun run(): Flow<Unit> = emptyFlow()
    }
    val transformed1 = source1.transform { flow -> flow.conflate() }
    val transformed2 = source2.transform { flow -> flow.conflate() }

    assertFalse(transformed1.doesSameWorkAs(transformed2))
  }

  @Test fun `transformed workers transform flows`() {
    val source = flowOf(1, 2, 3).asWorker()
    val transformed = source.transform { flow -> flow.map { it.toString() } }

    val transformedValues = runBlocking {
      transformed.run()
          .toList()
    }

    assertEquals(listOf("1", "2", "3"), transformedValues)
  }
}
