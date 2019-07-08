@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.legacy.Workflow
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.junit.Test

class WorkflowOperatorsTest {

  @Test fun `switch map state works`() {
    val downstreamTrigger = PublishSubject.create<Unit>()
    val workflow = object : Workflow<String, Nothing, Nothing>,
        Deferred<Nothing> by CompletableDeferred() {
      // Return a channel with a single item.
      override fun openSubscriptionToState(): ReceiveChannel<String> =
        Channel<String>(capacity = 1)
            .apply { offer("Hello") }

      override fun sendEvent(event: Nothing) {
      }
    }

    val mapped = workflow.switchMapState {
      var n = 0
      Observable.fromIterable(it.asIterable())
          // Emit characters one-at-a-time.
          .delay { downstreamTrigger.skip(n++.toLong()) }
    }

    @Suppress("UNCHECKED_CAST")
    val states = mapped.state.test() as TestObserver<Char>

    downstreamTrigger.onNext(Unit)
    assertThat(states.values().last()).isEqualTo('H')
    downstreamTrigger.onNext(Unit)
    assertThat(states.values().last()).isEqualTo('e')
    downstreamTrigger.onNext(Unit)
    assertThat(states.values().last()).isEqualTo('l')
    downstreamTrigger.onNext(Unit)
    assertThat(states.values().last()).isEqualTo('l')
    downstreamTrigger.onNext(Unit)
    assertThat(states.values().last()).isEqualTo('o')
  }

  @Test fun `switchMapState disposes observable when cancelled`() {
    val workflow = object : Workflow<Unit, Nothing, Nothing>,
        Deferred<Nothing> by CompletableDeferred() {
      // Return a channel with a single item.
      override fun openSubscriptionToState(): ReceiveChannel<Unit> =
        Channel<Unit>(capacity = 1)
            .apply { offer(Unit) }

      override fun sendEvent(event: Nothing) {
      }
    }
    var subscribeCount = 0
    var disposeCount = 0
    val mapped = workflow.switchMapState {
      Observable.never<Unit>()
          .doOnSubscribe { subscribeCount++ }
          .doOnDispose { disposeCount++ }
    }

    assertThat(subscribeCount).isEqualTo(0)
    assertThat(disposeCount).isEqualTo(0)

    val states = mapped.openSubscriptionToState()

    assertThat(subscribeCount).isEqualTo(1)
    assertThat(disposeCount).isEqualTo(0)

    states.cancel()

    assertThat(subscribeCount).isEqualTo(1)
    assertThat(disposeCount).isEqualTo(1)
  }
}
