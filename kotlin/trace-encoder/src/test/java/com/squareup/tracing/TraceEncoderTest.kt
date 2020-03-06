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
package com.squareup.tracing

import com.squareup.tracing.TraceEvent.Instant
import kotlinx.coroutines.runBlocking
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.microseconds

@OptIn(ExperimentalTime::class)
class TraceEncoderTest {

  /**
   * [TimeMark] that always returns [now] as [elapsedNow].
   */
  private class FakeTimeMark : TimeMark() {
    var now: Duration = 0.microseconds
    override fun elapsedNow(): Duration = now
  }

  @Test fun `multiple events sanity check`() {
    val firstBatch = listOf(
        traceEvent("one"),
        traceEvent("two")
    )
    val secondBatch = listOf(traceEvent("three"))

    val buffer = Buffer()
    runBlocking {
      val fakeTimeMark = FakeTimeMark()
      val encoder = TraceEncoder(this, start = fakeTimeMark) { buffer }
      val logger = encoder.createLogger("process", "thread")

      fakeTimeMark.now = 1.microseconds
      logger.log(firstBatch)

      fakeTimeMark.now = 2.microseconds
      logger.log(secondBatch)

      encoder.close()
    }
    val serialized = buffer.readUtf8()

    val expectedValue = """
      [
      {"name":"process_name","ph":"M","ts":0,"pid":0,"tid":0,"args":{"name":"process"}},
      {"name":"thread_name","ph":"M","ts":0,"pid":0,"tid":0,"args":{"name":"thread"}},
      {"name":"one","ph":"i","ts":1,"pid":0,"tid":0,"s":"t","args":{}},
      {"name":"two","ph":"i","ts":1,"pid":0,"tid":0,"s":"t","args":{}},
      {"name":"three","ph":"i","ts":2,"pid":0,"tid":0,"s":"t","args":{}},

    """.trimIndent()

    assertEquals(expectedValue, serialized)
  }

  private fun traceEvent(name: String) = Instant(name = name)
}
