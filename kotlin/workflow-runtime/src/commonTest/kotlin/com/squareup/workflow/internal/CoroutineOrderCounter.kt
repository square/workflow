package com.squareup.workflow.internal

/**
 * Simple atomic counter that can be used to assert ordering within coroutine tests.
 */
expect class CoroutineOrderCounter() {
  fun getAndIncrement(): Int
}
