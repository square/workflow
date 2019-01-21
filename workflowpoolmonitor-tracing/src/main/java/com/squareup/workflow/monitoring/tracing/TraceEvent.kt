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
package com.squareup.workflow.monitoring.tracing

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.ToJson
import com.squareup.workflow.monitoring.tracing.TraceEvent.Phase
import com.squareup.workflow.monitoring.tracing.TraceEvent.Scope

internal data class TraceEvent(
  val name: String,
  @Json(name = "cat") val category: String? = null,
  @Json(name = "ph") val phase: Phase,
  @Json(name = "ts") val timestamp: Long,
  @Json(name = "pid") val processId: Int,
  @Json(name = "tid") val threadId: Int,
  /** Only used for [Phase.INSTANT] events. */
  @Json(name = "s") val scope: Scope? = null,
  val args: Map<String, Any>? = null
) {
  enum class Phase(val code: Char) {
    DURATION_BEGIN('B'),
    DURATION_END('E'),
    COMPLETE('X'),
    INSTANT('i'),
    COUNTER('C'),
    ASYNC_BEGIN('b'),
    ASYNC_INSTANT('n'),
    ASYNC_END('e'),
  }

  enum class Scope(val code: Char) {
    GLOBAL('g'),
    PROCESS('p'),
    THREAD('t')
  }
}

internal object PhaseAdapter {
  @ToJson fun toJson(phase: Phase) = phase.code
  @FromJson fun fromJson(code: Char) = Phase.values().single { it.code == code }
}

internal object ScopeAdapter {
  @ToJson fun toJson(scope: Scope) = scope.code
  @FromJson fun fromJson(code: Char) = Scope.values().single { it.code == code }
}
