package com.squareup.workflow

import com.squareup.reactor.EnterState
import com.squareup.reactor.FinishWith
import com.squareup.reactor.Reaction
import com.squareup.workflow.rx2.ComposedReactor
import com.squareup.workflow.rx2.EventChannel
import com.squareup.workflow.rx2.doLaunch
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Type
import com.squareup.workflow.rx2.Workflow
import com.squareup.workflow.rx2.toCompletable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
class WorkflowPoolTest {
  companion object {
    const val NEW = "*NEW*"
    const val STOP = "*STOP*"
  }

  val eventsSent = mutableListOf<String>()

  var abandonCount = 0
  var launchCount = 0

  inner class MyReactor : ComposedReactor<String, String, String> {
    override val type = object : Type<String, String, String> {}

    override fun onReact(
      state: String,
      events: EventChannel<String>,
      workflows: WorkflowPool
    ): Single<out Reaction<String, String>> = events.select {
      onEvent<String> {
        eventsSent += it
        if (it == STOP) FinishWith(state) else EnterState(it)
      }
    }

    override fun launch(
      initialState: String,
      workflows: WorkflowPool
    ): Workflow<String, String, String> {
      launchCount++
      val workflow = doLaunch(initialState, workflows)
      return object : Workflow<String, String, String> by workflow {
        override fun abandon() {
          abandonCount++
          workflow.abandon()
        }
      }
    }
  }

  val myReactor = MyReactor()

  val pool = WorkflowPool()
      .apply { register(myReactor) }

  inner class DelegatingState(
    override val delegateState: String = "",
    name: String = ""
  ) : Delegating<String, String, String> {
    override val id: Id<String, String, String> = myReactor.type.makeId(name)
  }

  @Test fun metaTest_myReactorReportsStatesAndResult() {
    val workflow = myReactor.launch(NEW, pool)
    val stateSub = workflow.state.test() as TestObserver<String>
    val resultSub = workflow.result.test() as TestObserver<String>

    workflow.sendEvent("able")
    workflow.sendEvent("baker")
    workflow.sendEvent("charlie")
    workflow.sendEvent(STOP)

    with(stateSub) {
      assertValues(NEW, "able", "baker", "charlie")
      assertTerminated()
    }
    with(resultSub) {
      assertValue("charlie")
      assertTerminated()
    }
  }

  @Test fun metaTest_myReactorAbandonsAndStateCompletes() {
    val workflow = myReactor.launch(NEW, pool)
    val abandoned = AtomicBoolean(false)

    workflow.toCompletable()
        .subscribe { abandoned.set(true) }
    assertThat(abandoned.get()).isFalse()
    workflow.abandon()
    assertThat(abandoned.get()).isTrue()
  }

  @Test fun noEagerLaunch() {
    assertThat(launchCount).isZero()
  }

  @Test fun waitsForStateAfterCurrent() {
    val delegatingState = DelegatingState(NEW)
    val nestedStateSub = pool.nextDelegateReaction(delegatingState)
        .test()
    nestedStateSub.assertNoValues()

    val input = pool.input(delegatingState.id)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    nestedStateSub.assertNoValues()

    input.sendEvent("fnord")
    nestedStateSub.assertValue(EnterState("fnord"))
    nestedStateSub.assertComplete()
  }

  @Test fun reportsResult() {
    val firstState = DelegatingState(NEW)

    // We don't actually care about the reaction, just want the workflow
    // to start.
    pool.nextDelegateReaction(firstState)

    // Advance the state a bit.
    val input = pool.input(firstState.id)
    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    // Check that we get the result we expect after the above events.
    val resultSub = pool.nextDelegateReaction(DelegatingState("charlie"))
        .test()
    resultSub.assertNoValues()

    input.sendEvent(STOP)
    resultSub.assertValue(FinishWith("charlie"))
    resultSub.assertComplete()
  }

  @Test fun reportsImmediateResult() {
    val delegatingState = DelegatingState(NEW)
    val resultSub = pool.nextDelegateReaction(delegatingState)
        .test()
    resultSub.assertNoValues()

    val input = pool.input(delegatingState.id)
    input.sendEvent(STOP)
    resultSub.assertValue(FinishWith(NEW))
    resultSub.assertComplete()
  }

  @Test fun initsOncePerNextState() {
    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun initsOncePerResult() {
    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun routesEvents() {
    pool.nextDelegateReaction(DelegatingState())
    val input = pool.input(myReactor.type.makeId())

    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    assertThat(eventsSent).isEqualTo(listOf("able", "baker", "charlie"))
  }

  @Test fun dropsLateEvents() {
    val input = pool.input(myReactor.type.makeId())
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    input.sendEvent("charlie")
    assertThat(eventsSent).isEqualTo(
        listOf(
            "able", "baker",
            STOP
        )
    )
  }

  @Test fun dropsEarlyEvents() {
    val input = pool.input(myReactor.type.makeId())
    input.sendEvent("able")
    pool.nextDelegateReaction(DelegatingState())
    input.sendEvent("baker")

    assertThat(eventsSent).isEqualTo(listOf("baker"))
  }

  @Test fun resumesRoutingEvents() {
    pool.nextDelegateReaction(DelegatingState())
    val input = pool.input(myReactor.type.makeId())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)

    input.sendEvent("charlie")
    input.sendEvent("delta")
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("echo")
    input.sendEvent("foxtrot")

    assertThat(eventsSent).isEqualTo(
        listOf(
            "able", "baker",
            STOP, "echo", "foxtrot"
        )
    )
  }

  @Test fun abandonsOnlyOnce() {
    assertThat(abandonCount).isZero()
    pool.nextDelegateReaction(DelegatingState(NEW))
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    assertThat(abandonCount).isEqualTo(1)
  }

  @Test fun abandonEmitsNothingAndDoesNotComplete() {
    val alreadyInNewState = DelegatingState(NEW)
    val id = alreadyInNewState.id

    val stateSub = pool.nextDelegateReaction(alreadyInNewState)
        .test()

    pool.abandonDelegate(id)

    stateSub.assertNoValues()
    stateSub.assertNotTerminated()
  }
}
