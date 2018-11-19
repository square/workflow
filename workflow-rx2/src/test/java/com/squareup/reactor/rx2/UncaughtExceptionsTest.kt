package com.squareup.reactor.rx2

import junit.framework.TestCase.fail
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch

class UncaughtExceptionsTest {
  @Test fun rethrowingUncaughtExceptions_rethrowsExceptionFromBlock() {
    try {
      rethrowingUncaughtExceptions {
        throw RuntimeException("fail")
      }
      fail("Expected exception.")
    } catch (e: RuntimeException) {
      assertThat(e.message).isEqualTo("fail")
    }
  }

  @Test fun rethrowingUncaughtExceptions_rethrowsUncaught_fromSameThread() {
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

  @Test fun rethrowingUncaughtException_suppressesUncaught_whenBlockThrows() {
    try {
      rethrowingUncaughtExceptions {
        Thread.getDefaultUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), RuntimeException("fail2"))
        // Immediately-thrown exception is always given priority.
        throw RuntimeException("fail1")
      }
      fail("Expected exception.")
    } catch (e: RuntimeException) {
      assertThat(e.message).isEqualTo("fail1")
      assertThat(e.suppressed.single().message).isEqualTo("fail2")
    }
  }

  @Test fun rethrowingUncaughtException_suppressesUncaught_whenMultipleUncaught() {
    try {
      rethrowingUncaughtExceptions {
        Thread.getDefaultUncaughtExceptionHandler()
            .apply {
              uncaughtException(Thread.currentThread(), RuntimeException("fail1"))
              uncaughtException(Thread.currentThread(), RuntimeException("fail2"))
              uncaughtException(Thread.currentThread(), RuntimeException("fail3"))
            }
      }
      fail("Expected exception.")
    } catch (e: RuntimeException) {
      assertThat(e.message).isEqualTo("fail1")
      assertThat(e.suppressed!![0].message).isEqualTo("fail2")
      assertThat(e.suppressed!![1].message).isEqualTo("fail3")
    }
  }

  @Test fun rethrowingUncaughtException_suppressesUncaught_whenMultipleThreads() {
    // This number should be high enough to give some contention.
    val threadCount = 10
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
      assertThat(allMessages.distinct()).hasSameSizeAs(allMessages)
    }
  }
}
