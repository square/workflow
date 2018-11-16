package com.squareup.reactor.rx2

import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE
import io.reactivex.Single
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import org.jetbrains.annotations.TestOnly

/**
 * Helper for [Rx2Reactor]s that can accept events from external sources.
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
interface Rx2EventChannel<E : Any> {
  fun <R : Any> select(block: EventSelectBuilder<E, R>.() -> Unit): Single<out R>
}

// TODO(MDX-166) This is only public for test access, should be internal when buck is fixed.
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
fun <E : Any> ReceiveChannel<E>.asEventChannel() = object : Rx2EventChannel<E> {
  /**
   * Returns a [Single] that will complete successfully when [Rx2Workflow.sendEvent] is passed an
   * event that matches one of the select cases specified by the given block.
   *
   * Exceptions thrown from `block` are thrown immediately, exceptions thrown from select cases are
   * emitted by the returned `Single`.
   *
   * This method will throw if called multiple times before either:
   * 1. an acceptable event is sent.
   * 2. the returned `Single` is unsubscribed from. This allows you to compose the resulting single
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
    // ensuring events are sent from the correct threads.
    return GlobalScope.rxSingle<R>(Dispatchers.Unconfined) {
      // We pass this job to the EventSelectBuilder so it can use it as the parent for any coroutines
      // it starts. We cancel the job after a selection is made so we don't leak, e.g., rx
      // subscriptions. We also make it a child of the rxSingle job so that if there's an exception,
      // it'll get cancelled automatically.
      val selectionJob = Job(parent = coroutineContext[Job])
      kotlinx.coroutines.experimental.selects.select<Single<R>> {
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
    }
  }
}

// Helpers for testing.

/**
 * **Helper for testing:**
 * Creates an [Rx2EventChannel] that will send all the values passed, and then throw if another
 * select is attempted.
 */
@TestOnly fun <E : Any> eventChannelOf(vararg values: E): Rx2EventChannel<E> =
  Channel<E>(values.size)
      .apply {
        // Load all the values into the channel's buffer.
        values.forEach { check(offer(it)) }
        // Ensure any receives after the values are read will fail.
        close()
      }
      .asEventChannel()
