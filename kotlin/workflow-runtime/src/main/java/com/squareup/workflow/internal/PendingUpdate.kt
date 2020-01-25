package com.squareup.workflow.internal

/**
 * TODO write documentation
 */
internal typealias PendingUpdate = () -> Any?

interface PendingUpdateSink {
  /**
   * Enqueues [update] and suspends until the update has been accepted for execution.
   *
   * Use this method to accept backpressure from the runtime when there is contention.
   */
  suspend fun update(update: PendingUpdate)

  /**
   * Enqueues [update] and returns immediately.
   *
   * Use this method for updates triggered from non-coroutine contexts, such as UI events.
   */
  fun enqueueUpdate(update: PendingUpdate)
}
