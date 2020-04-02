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
package com.squareup.workflow.internal

import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener

/**
 * Diagnostic listener that records all received events in a list for testing.
 */
class RecordingDiagnosticListener : SimpleLoggingDiagnosticListener() {

  private var events: List<String> = emptyList()

  override fun println(text: String) {
    events = events + text
  }

  fun consumeEvents(): List<String> = events
      .also { events = emptyList() }

  fun consumeEventNames(): List<String> = consumeEvents().map { it.substringBefore('(') }

  fun consumeNextEvent(): String = events.first()
      .also { events = events.subList(1, events.size) }
}
