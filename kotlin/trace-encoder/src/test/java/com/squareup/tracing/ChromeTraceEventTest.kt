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

import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_PROCESS
import com.squareup.tracing.ChromeTraceEvent.Phase.ASYNC_BEGIN
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals

class ChromeTraceEventTest {

  @Test fun `serialization golden value`() {
    val traceEvent = ChromeTraceEvent(
        name = "name",
        category = "category",
        phase = ASYNC_BEGIN,
        timestampMicros = 123456,
        processId = 1,
        threadId = 1,
        id = -123L,
        scope = INSTANT_SCOPE_PROCESS,
        args = mapOf("key" to "value")
    )
    val serialized = Buffer()
        .also { traceEvent.writeTo(it) }
        .readUtf8()
    val expectedValue =
      """{"name":"name","cat":"category","ph":"b","ts":123456,"pid":1,"tid":1,"id":-123,""" +
          """"s":"p","args":{"key":"value"}}"""

    assertEquals(expectedValue, serialized)
  }
}
