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
package com.squareup.workflow.v2.internal

import com.squareup.workflow.v2.ChannelUpdate
import com.squareup.workflow.v2.ChannelUpdate.Closed
import com.squareup.workflow.v2.ChannelUpdate.Value
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ChannelUpdatesTest {

  @Test fun suspends() {
    val channel = Channel<String>(1)

    runBlocking {
      val result = GlobalScope.async(Dispatchers.Unconfined) {
        select<ChannelUpdate<String>> {
          onChannelUpdate(channel) { it }
        }
      }

      assertFalse(result.isCompleted)

      yield()

      assertFalse(result.isCompleted)

      result.cancel()
    }
  }

  @Test fun emitsValue() {
    val channel = Channel<String>(1)
    channel.offer("foo")

    val result = runBlocking {
      select<ChannelUpdate<String>> {
        onChannelUpdate(channel) { it }
      }
    }

    assertEquals(Value("foo"), result)
  }

  @Test fun emitsClose() {
    val channel = Channel<String>()
    channel.close()

    val result = runBlocking {
      select<ChannelUpdate<String>> {
        onChannelUpdate(channel) { it }
      }
    }

    assertEquals(Closed, result)
  }

  @Test fun handlesError() {
    val channel = Channel<String>()
    channel.cancel(ExpectedException())

    assertFailsWith<ExpectedException> {
      runBlocking {
        select<ChannelUpdate<String>> {
          onChannelUpdate(channel) { it }
        }
      }
    }
  }

  private class ExpectedException : RuntimeException()
}
