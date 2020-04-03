package com.squareup.workflow.internal

import kotlinx.coroutines.CoroutineScope

actual fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T =
  kotlinx.coroutines.runBlocking(block = block)
