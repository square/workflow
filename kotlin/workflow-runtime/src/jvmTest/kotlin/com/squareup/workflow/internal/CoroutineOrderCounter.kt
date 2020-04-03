package com.squareup.workflow.internal

import java.util.concurrent.atomic.AtomicInteger

actual class CoroutineOrderCounter actual constructor() {

  private val counter = AtomicInteger(0)

  actual fun getAndIncrement(): Int = counter.getAndIncrement()
}
