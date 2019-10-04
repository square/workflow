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

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_THREAD
import com.squareup.tracing.ChromeTraceEvent.Phase
import com.squareup.tracing.ChromeTraceEvent.Phase.METADATA
import okio.BufferedSink

/**
 * JSON-serializable model of a `chrome://tracing` event.
 *
 * Documentation of event format is available
 * [here](https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU).
 */
internal data class ChromeTraceEvent(
  @Json(name = "name") val name: String,
  @Json(name = "cat") val category: String? = null,
  @Json(name = "ph") val phase: Phase,
  @Json(name = "ts") val timestampMicros: Long,
  @Json(name = "pid") val processId: Int = 0,
  @Json(name = "tid") val threadId: Int = 0,
  /** Only used for ASYNC events. */
  @Json(name = "id") val id: Any? = null,
  /**
   * Only used for [Phase.INSTANT] events.
   * See [INSTANT_SCOPE_THREAD], etc.
   */
  @Json(name = "s") val scope: Char? = null,
  @Json(name = "args") val args: Map<String, Any?>? = null
) {

  @Suppress("unused")
  enum class Phase(internal val code: Char) {
    DURATION_BEGIN('B'),
    DURATION_END('E'),
    COMPLETE('X'),
    INSTANT('i'),
    COUNTER('C'),
    ASYNC_BEGIN('b'),
    ASYNC_INSTANT('n'),
    ASYNC_END('e'),
    OBJECT_CREATED('N'),
    OBJECT_SNAPSHOT('O'),
    OBJECT_DESTROYED('D'),
    METADATA('M')
  }

  /**
   * Writes this event to a trace file using [jsonAdapter].
   */
  fun writeTo(sink: BufferedSink) = jsonAdapter.toJson(sink, this)

  companion object {
    const val INSTANT_SCOPE_THREAD = 't'
    const val INSTANT_SCOPE_PROCESS = 'p'
    const val INSTANT_SCOPE_GLOBAL = 'g'

    private val jsonAdapter: JsonAdapter<ChromeTraceEvent> by lazy {
      val moshi = Moshi.Builder()
          .add(PhaseAdapter)
          .add(KotlinJsonAdapterFactory())
          .build()
      return@lazy moshi.adapter(ChromeTraceEvent::class.java)
    }
  }
}

@Suppress("unused")
private object PhaseAdapter {
  @ToJson fun toJson(phase: Phase) = phase.code
  @FromJson fun fromJson(code: Char) = Phase.values().single { it.code == code }
}

internal fun createProcessNameEvent(
  name: String,
  processId: Int,
  timestampMicros: Long
): ChromeTraceEvent = ChromeTraceEvent(
    name = "process_name",
    phase = METADATA,
    processId = processId,
    args = mapOf("name" to name),
    timestampMicros = timestampMicros
)

internal fun createThreadNameEvent(
  name: String,
  processId: Int,
  threadId: Int,
  timestampMicros: Long
): ChromeTraceEvent = ChromeTraceEvent(
    name = "thread_name",
    phase = METADATA,
    processId = processId,
    threadId = threadId,
    args = mapOf("name" to name),
    timestampMicros = timestampMicros
)
