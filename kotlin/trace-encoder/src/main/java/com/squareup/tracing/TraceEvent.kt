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

import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_GLOBAL
import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_PROCESS
import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_THREAD
import com.squareup.tracing.ChromeTraceEvent.Phase.ASYNC_BEGIN
import com.squareup.tracing.ChromeTraceEvent.Phase.ASYNC_END
import com.squareup.tracing.ChromeTraceEvent.Phase.COUNTER
import com.squareup.tracing.ChromeTraceEvent.Phase.DURATION_BEGIN
import com.squareup.tracing.ChromeTraceEvent.Phase.DURATION_END
import com.squareup.tracing.ChromeTraceEvent.Phase.INSTANT
import com.squareup.tracing.ChromeTraceEvent.Phase.OBJECT_CREATED
import com.squareup.tracing.ChromeTraceEvent.Phase.OBJECT_DESTROYED
import com.squareup.tracing.ChromeTraceEvent.Phase.OBJECT_SNAPSHOT
import com.squareup.tracing.TraceEvent.AsyncDurationBegin
import com.squareup.tracing.TraceEvent.AsyncDurationEnd
import com.squareup.tracing.TraceEvent.Counter
import com.squareup.tracing.TraceEvent.DurationBegin
import com.squareup.tracing.TraceEvent.DurationEnd
import com.squareup.tracing.TraceEvent.Instant
import com.squareup.tracing.TraceEvent.Instant.InstantScope.GLOBAL
import com.squareup.tracing.TraceEvent.Instant.InstantScope.PROCESS
import com.squareup.tracing.TraceEvent.Instant.InstantScope.THREAD
import com.squareup.tracing.TraceEvent.ObjectCreated
import com.squareup.tracing.TraceEvent.ObjectDestroyed
import com.squareup.tracing.TraceEvent.ObjectSnapshot

/**
 * Represents a single event in a trace.
 */
sealed class TraceEvent {

  open val category: String? get() = null

  data class DurationBegin(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  data class DurationEnd(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  data class Instant(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    val scope: InstantScope = THREAD,
    override val category: String? = null
  ) : TraceEvent() {
    enum class InstantScope {
      THREAD,
      PROCESS,
      GLOBAL
    }
  }

  data class AsyncDurationBegin(
    val id: Any,
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  data class AsyncDurationEnd(
    val id: Any,
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  data class ObjectCreated(
    val id: Long,
    val objectType: String
  ) : TraceEvent()

  data class ObjectDestroyed(
    val id: Long,
    val objectType: String
  ) : TraceEvent()

  data class ObjectSnapshot(
    val id: Long,
    val objectType: String,
    val snapshot: Any
  ) : TraceEvent()

  data class Counter(
    val name: String,
    val series: Map<String, Number>,
    val id: Long? = null
  ) : TraceEvent()
}

internal fun TraceEvent.toChromeTraceEvent(
  threadId: Int,
  processId: Int,
  nowMicros: Long
): ChromeTraceEvent = when (this) {
  is DurationBegin -> ChromeTraceEvent(
      phase = DURATION_BEGIN,
      name = name,
      category = category,
      args = args,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is DurationEnd -> ChromeTraceEvent(
      phase = DURATION_END,
      name = name,
      category = category,
      args = args,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is Instant -> ChromeTraceEvent(
      phase = INSTANT,
      name = name,
      category = category,
      scope = when (scope) {
        THREAD -> INSTANT_SCOPE_THREAD
        PROCESS -> INSTANT_SCOPE_PROCESS
        GLOBAL -> INSTANT_SCOPE_GLOBAL
      },
      args = args,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is AsyncDurationBegin -> ChromeTraceEvent(
      phase = ASYNC_BEGIN,
      id = id,
      name = name,
      category = category,
      args = args,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is AsyncDurationEnd -> ChromeTraceEvent(
      phase = ASYNC_END,
      id = id,
      name = name,
      category = category,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is ObjectCreated -> ChromeTraceEvent(
      phase = OBJECT_CREATED,
      id = id.toHex(),
      name = objectType,
      category = category,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is ObjectDestroyed -> ChromeTraceEvent(
      phase = OBJECT_DESTROYED,
      id = id.toHex(),
      name = objectType,
      category = category,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is ObjectSnapshot -> ChromeTraceEvent(
      phase = OBJECT_SNAPSHOT,
      id = id.toHex(),
      name = objectType,
      category = category,
      args = mapOf("snapshot" to snapshot),
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
  is Counter -> ChromeTraceEvent(
      phase = COUNTER,
      id = id?.toHex(),
      name = name,
      args = series,
      threadId = threadId,
      processId = processId,
      timestampMicros = nowMicros
  )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Long.toHex() = toString(16)
