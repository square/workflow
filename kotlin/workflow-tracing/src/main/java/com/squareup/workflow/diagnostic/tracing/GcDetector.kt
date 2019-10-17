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
package com.squareup.workflow.diagnostic.tracing

internal typealias GcDetectorConstructor = (onGcDetected: () -> Unit) -> GcDetector

/**
 * Class that does rough logging of garbage collection runs by allocating an unowned object that
 * logs a trace event when its finalizer is ran.
 *
 * Internal and open for testing.
 */
internal open class GcDetector(private val onGcDetected: () -> Unit) {

  @Volatile private var running = true

  private inner class GcCanary {
    @Throws(Throwable::class) protected fun finalize() {
      if (!running) return

      onGcDetected()
      GcCanary()
    }
  }

  init {
    GcCanary()
  }

  fun stop() {
    running = false
  }
}
