package com.squareup.workflow.rx2

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test

class ReproInlineBug {

  inline fun foo(noinline block: suspend () -> Unit) = suspend { block() }

  inline fun bar(crossinline block: suspend () -> Unit) = foo { block() }

  inline fun baz() = bar {
    try {
      // This _should_ suspend forever, but it resumes immediately.
      suspendCancellableCoroutine<Unit> {}
    } catch (e: Throwable) {
      println("this should never execute")
    }
    println("nor should this")
  }

  @Test fun demonstrate() {
    val waiter = baz()

    runBlocking {
      // This should suspend forever.
      waiter()
    }
  }
}
