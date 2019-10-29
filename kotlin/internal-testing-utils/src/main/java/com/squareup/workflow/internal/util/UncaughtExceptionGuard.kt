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
package com.squareup.workflow.internal.util

import java.util.concurrent.atomic.AtomicReference

/**
 * Helper for collecting uncaught exceptions and reporting them during the execution of some
 * function.
 *
 * As a convenience, [rethrowingUncaughtExceptions] will rethrow exceptions reported to the
 * [default `Thread` uncaught exception handler][Thread.getDefaultUncaughtExceptionHandler].
 *
 * ## Usage
 *
 * 1. Register an uncaught exception handler to report uncaught exceptions with [reportUncaught].
 * 2. Run your code with [runRethrowingUncaught]. Any exceptions passed to [reportUncaught] will be
 * thrown after the block returns. If the block itself throws, that exception will be rethrown
 * directly with uncaught exceptions added to its [suppressed list][Throwable.addSuppressed].
 */
class UncaughtExceptionGuard {
  private val uncaughtException = AtomicReference<Throwable>()

  fun reportUncaught(e: Throwable) {
    if (!uncaughtException.compareAndSet(null, e)) {
      // If another thread beat us to it, add this exception to the suppressed list.
      // addSuppressed is thread-safe so we don't need to do any explicit synchronization.
      uncaughtException.get()!!.addSuppressedSafely(e)
    }
  }

  fun <T> runRethrowingUncaught(block: () -> T): T {
    val result = try {
      block()
    } catch (e: Throwable) {
      // If any uncaught exceptions were reported during block's execution, mark them as suppressed.
      // Treat the exception from this try/catch as the "primary" one because it usually indicates
      // an assertion failure or something, which is the most interesting error.
      e.addSuppressedSafely(uncaughtException.get())
      throw e
    }

    // If the block completed successfully, but an uncaught exception was reported, report it now.
    uncaughtException.get()
        ?.let { throw it }

    return result
  }

  /**
   * Adds `e` as a suppressed exception to this one, unless `e` is null or the same object as this.
   */
  private fun Throwable.addSuppressedSafely(e: Throwable?) {
    if (e != null && e !== this) addSuppressed(e)
  }
}

/**
 * Temporarily overrides the
 * [default uncaught exception handler][Thread.defaultUncaughtExceptionHandler] while running [block],
 * and then after `block` returns, rethrows any exceptions that were reported to the uncaught handler.
 *
 * This allows the normal JUnit exception assertion mechanisms to work with uncaught exceptions.
 */
fun rethrowingUncaughtExceptions(block: () -> Unit) {
  val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
  val guard = UncaughtExceptionGuard()

  Thread.setDefaultUncaughtExceptionHandler { _, e ->
    guard.reportUncaught(e)
  }
  try {
    guard.runRethrowingUncaught(block)
  } finally {
    Thread.setDefaultUncaughtExceptionHandler(oldHandler)
  }
}
