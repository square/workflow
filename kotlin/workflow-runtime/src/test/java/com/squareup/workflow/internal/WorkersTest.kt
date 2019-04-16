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

import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Worker.OutputOrFinished.Finished
import com.squareup.workflow.Worker.OutputOrFinished.Output
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class WorkersTest {

  @Test fun suspends() {
    val channel = Channel<Output<String>>(1)

    runBlocking {
      val result = GlobalScope.async(Dispatchers.Unconfined) {
        select<OutputOrFinished<String>> {
          onReceiveOutputOrFinished(channel) { it }
        }
      }

      assertFalse(result.isCompleted)

      yield()

      assertFalse(result.isCompleted)

      result.cancel()
    }
  }

  @Test fun `emits output`() {
    val channel = Channel<Output<String>>(1)
    channel.offer(Output("foo"))

    val result = runBlocking {
      select<OutputOrFinished<String>> {
        onReceiveOutputOrFinished(channel) { it }
      }
    }

    assertEquals(Output("foo"), result)
  }

  @Test fun `emits finished`() {
    val channel = Channel<Output<String>>(0)
    channel.close()

    val result = runBlocking {
      select<OutputOrFinished<String>> {
        onReceiveOutputOrFinished(channel) { it }
      }
    }

    assertEquals(Finished, result)
  }

  @Test fun `handles error`() {
    val channel = Channel<Output<String>>()
    // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
    @Suppress("DEPRECATION")
    channel.cancel(ExpectedException())

    assertFailsWith<ExpectedException> {
      runBlocking {
        select<OutputOrFinished<String>> {
          onReceiveOutputOrFinished(channel) { it }
        }
      }
    }
  }

  private class ExpectedException : RuntimeException()
}
