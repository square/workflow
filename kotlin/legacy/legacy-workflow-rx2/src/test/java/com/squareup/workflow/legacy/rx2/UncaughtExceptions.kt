/*
 * Copyright 2017 Square Inc.
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
package com.squareup.workflow.legacy.rx2

import java.util.concurrent.atomic.AtomicReference

/**
 * Temporarily overrides the
 * [default uncaught exception handler][Thread.defaultUncaughtExceptionHandler] while running [block],
 * and then after `block` returns, rethrows any exceptions that were reported to the uncaught handler.
 *
 * This allows the normal JUnit exception assertion mechanisms to work with uncaught exceptions.
 */
fun rethrowingUncaughtExceptions(block: () -> Unit) {
  val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
  val uncaughtException = AtomicReference<Throwable?>(null)
  Thread.setDefaultUncaughtExceptionHandler { _, e ->
    // If this is the first uncaught exception, try to set it as the initial exception.
    if (!uncaughtException.compareAndSet(null, e)) {
      // If another thread beat us to it, add this exception to the suppressed list.
      // addSuppressed is thread-safe so we don't need to do any explicit synchronization.
      uncaughtException.get()!!.addSuppressed(e)
    }
  }
  var caughtException: Throwable? = null

  try {
    block()
  } catch (e: Throwable) {
    // We can't just re-throw this exception immediately, because we need to first see if any other
    // uncaught exceptions were reported so we can add them to this one as suppressed exceptions.
    caughtException = e
  } finally {
    Thread.setDefaultUncaughtExceptionHandler(oldHandler)

    val uncaught = uncaughtException.get()
    if (caughtException != null) {
      // Uncaught exceptions were reported, AND the block threw its own exception. The uncaught ones
      // are "suppressed" by the block's exception.
      if (uncaught != null) caughtException.addSuppressed(uncaught)
      throw caughtException
    }

    // The block completed successfully, so if there were any uncaught exceptions we need to throw
    // them now.
    if (uncaught != null) throw uncaught
  }
}
