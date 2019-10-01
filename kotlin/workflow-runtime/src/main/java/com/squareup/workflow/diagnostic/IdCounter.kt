package com.squareup.workflow.diagnostic

/**
 * TODO write documentation
 */
internal class IdCounter {

  private var nextId = 0L

  fun createId(): Long = nextId++
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun IdCounter?.createId(): Long = this?.createId() ?: 0
