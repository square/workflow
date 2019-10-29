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
package com.squareup.workflow.internal.util

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.test.assertFailsWith

class UncaughtExceptionGuardTest {

  private val guard = UncaughtExceptionGuard()

  @Test fun `UncaughtExceptionGuard rethrows Exception from block`() {
    val error = assertFailsWith<ExpectedException> {
      guard.runRethrowingUncaught {
        throw ExpectedException("fail")
      }
    }
    assertThat(error.message).isEqualTo("fail")
  }

  @Test fun `UncaughtExceptionGuard suppresses Uncaught when block throws`() {
    guard.reportUncaught(ExpectedException("fail2"))
    val error = assertFailsWith<ExpectedException> {
      guard.runRethrowingUncaught {
        // Immediately-thrown exception is always given priority.
        throw ExpectedException("fail1")
      }
    }
    assertThat(error.message).isEqualTo("fail1")
    assertThat(error.suppressed.single().message).isEqualTo("fail2")
  }

  @Test fun `UncaughtExceptionGuard suppresses Uncaught when multiple Uncaught`() {
    guard.reportUncaught(ExpectedException("fail1"))
    guard.reportUncaught(ExpectedException("fail2"))
    guard.reportUncaught(ExpectedException("fail3"))
    val error = assertFailsWith<ExpectedException> {
      guard.runRethrowingUncaught { }
    }
    assertThat(error.message).isEqualTo("fail1")
    assertThat(error.suppressed!![0].message).isEqualTo("fail2")
    assertThat(error.suppressed!![1].message).isEqualTo("fail3")
  }

  @Test fun `rethrowingUncaughtExceptions rethrows Uncaught from same thread`() {
    try {
      rethrowingUncaughtExceptions {
        Thread.getDefaultUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), RuntimeException("fail"))
      }
      fail("Expected exception.")
    } catch (e: RuntimeException) {
      assertThat(e.message).isEqualTo("fail")
    }
  }

  @Test fun `rethrowingUncaughtException suppresses Uncaught when multiple threads`() {
    // This number should be high enough to give some contention.
    val threadCount = 50
    val readyToStartLatch = CountDownLatch(threadCount)
    val startLatch = CountDownLatch(1)
    val finishedLatch = CountDownLatch(threadCount)
    repeat(threadCount) { i ->
      Thread {
        readyToStartLatch.countDown()
        // Wait for all the other threads are also ready…
        startLatch.await()
        Thread.getDefaultUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), RuntimeException("fail $i"))
        finishedLatch.countDown()
      }.start()
    }
    readyToStartLatch.await()

    try {
      rethrowingUncaughtExceptions {
        // Let all the threads report exceptions in parallel.
        startLatch.countDown()
        // Give everyone a chance to throw…
        finishedLatch.await()
      }
    } catch (e: RuntimeException) {
      val allMessages = e.suppressed.map { it.message } + e.message

      // Ensure that all exceptions were reported…
      assertThat(allMessages).hasSize(threadCount)

      // Ensure that each exception was only reported once…
      assertThat(allMessages.distinct()).hasSize(allMessages.size)
    }
  }

  private class ExpectedException(message: String? = null) : RuntimeException(message)
}
