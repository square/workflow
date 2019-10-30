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

/**
 * [Logs][log] [TraceEvent]s to a [TraceEncoder] under a given process and thread name.
 *
 * Create with [TraceEncoder.createLogger].
 */
interface TraceLogger {

  /**
   * Tags all events with the current timestamp and then enqueues them to be written to the trace
   * file.
   */
  fun log(eventBatch: List<TraceEvent>)

  /**
   * Tags event with the current timestamp and then enqueues it to be written to the trace
   * file.
   */
  fun log(event: TraceEvent)
}
