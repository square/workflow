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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy.rx2

import io.reactivex.Single
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxSingle
import kotlin.coroutines.suspendCoroutine

/**
 * Helper for [Reactor]s that can accept events from external sources.
 *
 * From inside `onReact`, you can handle events by calling `select` and passing a block that describes
 * how to handle each event type you're willing to accept. A [Single] will be returned that will
 * complete as soon as a matching event (or other clause, see below) is selected. Any exceptions
 * thrown from within your event handler blocks, or the `select` block itself, will be reported
 * through that `Single`.
 *
 * For example,
 * ```
 * events.select {
 *   onEvent<ButtonClicked> { EnterState(ButtonHasBeenClicked) }
 *   onEvent<BackPressed> { FinishWith(Canceled) }
 * }
 * ```
 *
 * You can also simultaneously wait for [Single]s to complete by calling `onSuccess` alongside your
 * `onEvent`s.
 *
 * When you specify at least one `onEvent` clause, if the next event sent down the channel doesn't
 * match any of your clauses, the `Single` will complete with an [IllegalStateException].
 *
 * If, at the time an event is sent, the reactor is not actively listening for events, the
 * event will be queued up and delivered as soon as the reactor asks for the next event.
 * Note that when the reactor _does_ get around to processing the next event, if it doesn't match
 * any of its cases, it will still throw. Events are not re-ordered or re-enqueued to try to find a
 * matching event.
 *
 * @param E The supertype of all valid events. Probably a sealed class.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
interface EventChannel<E : Any> {
  fun <R : Any> select(block: EventSelectBuilder<E, R>.() -> Unit): Single<out R>
}

@Deprecated("Use com.squareup.workflow.Workflow")
fun <E : Any> ReceiveChannel<E>.asEventChannel() = object : EventChannel<E> {
  /**
   * Returns a [Single] that will complete successfully when [Workflow.sendEvent] is passed an
   * event that matches one of the select cases specified by the given block.
   *
   * Exceptions thrown from `block` are thrown immediately, exceptions thrown from select cases are
   * emitted by the returned `Single`.
   *
   * This method will throw if called multiple times before either:
   * 1. an acceptable event is sent.
   * 2. the returned `Single` is unsubscribed from. This allows you to render the resulting single
   *    with other `Single`s that will preempt our emitting (e.g. timing out waiting for an event).
   *
   * If the workflow is abandoned while waiting for an event, the single will never complete. It will
   * be unsubscribed from immediately instead.
   */
  override fun <R : Any> select(block: EventSelectBuilder<E, R>.() -> Unit): Single<out R> {
    // Using GlobalScope is generally a bad practice. However, it is appropriate here because
    // it is only being used to produce a Single. Rx code already has to be responsible enough to
    // always subscribe to and dispose of things correctly, so we don't need to rely on structured
    // concurrency.
    // Furthermore, we use the Unconfined dispatcher since we don't do any real work here ourselves,
    // and any threading requirements of the calling code can be enforced using Rx mechanisms and
    // ensuring events are sent from the correct threads. See the README in workflow-core for more
    // information.
    return rxSingle(Unconfined) {
      try {
        // We pass this job to the EventSelectBuilder so it can use it as the parent for any coroutines
        // it starts. We cancel the job after a selection is made so we don't leak, e.g., rx
        // subscriptions. We also make it a child of the rxSingle job so that if there's an exception,
        // it'll get cancelled automatically.
        val selectionJob = Job(parent = coroutineContext[Job])
        kotlinx.coroutines.selects.select<Single<R>> {
          val selectBuilder = this
          val eventSelectBuilder = EventSelectBuilder<E, R>(selectBuilder, selectionJob)
          block(eventSelectBuilder)

          // Always await the next event, even if there are no event cases registered.
          // See this PR comment: https://git.sqcorp.co/projects/ANDROID/repos/register/pull-requests/23262/overview?commentId=2780276
          // If the event doesn't match any registered cases, throw.
          onReceiveOrNull { event ->
            if (event == null) {
              // Channel was closed while we were suspended.
              // The single is about to be unsubscribed from, so don't emit.
              return@onReceiveOrNull Single.never<R>()
            }

            eventSelectBuilder.cases.asSequence()
                .mapNotNull { it.tryHandle(event) }
                .firstOrNull()
                ?.invoke()
                ?.let { Single.just(it) }
                ?: throw IllegalStateException("Expected EventChannel to accept event: $event")
          }
        }
            .await()
            // In the happy case, we still need to cancel this job so any observable cases that didn't
            // win get unsubscribed.
            .also { selectionJob.cancel() }
      } catch (cancellation: CancellationException) {
        val cause = cancellation.unwrapRealCause()
        if (cause == null) {
          // The select was cancelled normally, which means the workflow was abandoned and we're
          // about to get unsubscribed from. Don't propagate the error, just never emit/return.
          suspendCoroutine<Nothing> { }
        } else {
          // The select actually failed, so rethrow the actual exception.
          throw cause
        }
      }
    }
  }

  /**
   * Searches up the cause chain to find the first exception that is not a [CancellationException]
   * and returns it. If every cause is a cancellation, returns null.
   */
  private fun CancellationException.unwrapRealCause(): Throwable? =
    generateSequence<Throwable>(this) { it.cause }
        .firstOrNull { it !is CancellationException }
}
