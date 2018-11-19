package com.squareup.workflow

import com.squareup.reactor.EnterState
import com.squareup.reactor.FinishWith
import com.squareup.reactor.Reaction
import com.squareup.reactor.rx2.Rx2ComposedReactor
import com.squareup.reactor.rx2.Rx2EventChannel
import com.squareup.reactor.rx2.doLaunch
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Type
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

  inner class MyReactor : Rx2ComposedReactor<String, String, String> {
    override val type = object : Type<String, String, String> {}

    override fun onReact(
      state: String,
      events: Rx2EventChannel<String>,
      workflows: WorkflowPool
    ): Single<out Reaction<String, String>> = events.select {
      onEvent<String> {
        eventsSent += it
        if (it == STOP) FinishWith(state) else EnterState(it)
      }
    }

    override fun launchRx2(
      initialState: String,
      workflows: WorkflowPool
    ): Rx2Workflow<String, String, String> {
      launchCount++
      val workflow = doLaunch(initialState, workflows)
      return object : Rx2Workflow<String, String, String> by workflow {
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
    val workflow = myReactor.launchRx2(NEW, pool)
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
    val workflow = myReactor.launchRx2(NEW, pool)
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
    val nestedStateSub = pool.nextDelegateReactionRx2(delegatingState)
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
    pool.nextDelegateReactionRx2(firstState)

    // Advance the state a bit.
    val input = pool.input(firstState.id)
    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    // Check that we get the result we expect after the above events.
    val resultSub = pool.nextDelegateReactionRx2(DelegatingState("charlie"))
        .test()
    resultSub.assertNoValues()

    input.sendEvent(STOP)
    resultSub.assertValue(FinishWith("charlie"))
    resultSub.assertComplete()
  }

  @Test fun reportsImmediateResult() {
    val delegatingState = DelegatingState(NEW)
    val resultSub = pool.nextDelegateReactionRx2(delegatingState)
        .test()
    resultSub.assertNoValues()

    val input = pool.input(delegatingState.id)
    input.sendEvent(STOP)
    resultSub.assertValue(FinishWith(NEW))
    resultSub.assertComplete()
  }

  @Test fun initsOncePerNextState() {
    pool.nextDelegateReactionRx2(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReactionRx2(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReactionRx2(DelegatingState())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun initsOncePerResult() {
    pool.nextDelegateReactionRx2(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReactionRx2(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReactionRx2(DelegatingState())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun routesEvents() {
    pool.nextDelegateReactionRx2(DelegatingState())
    val input = pool.input(myReactor.type.makeId())

    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    assertThat(eventsSent).isEqualTo(listOf("able", "baker", "charlie"))
  }

  @Test fun dropsLateEvents() {
    val input = pool.input(myReactor.type.makeId())
    pool.nextDelegateReactionRx2(DelegatingState())

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
    pool.nextDelegateReactionRx2(DelegatingState())
    input.sendEvent("baker")

    assertThat(eventsSent).isEqualTo(listOf("baker"))
  }

  @Test fun resumesRoutingEvents() {
    pool.nextDelegateReactionRx2(DelegatingState())
    val input = pool.input(myReactor.type.makeId())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)

    input.sendEvent("charlie")
    input.sendEvent("delta")
    pool.nextDelegateReactionRx2(DelegatingState())

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
    pool.nextDelegateReactionRx2(DelegatingState(NEW))
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    assertThat(abandonCount).isEqualTo(1)
  }

  @Test fun abandonEmitsNothingAndDoesNotComplete() {
    val alreadyInNewState = DelegatingState(NEW)
    val id = alreadyInNewState.id

    val stateSub = pool.nextDelegateReactionRx2(alreadyInNewState)
        .test()

    pool.abandonDelegate(id)

    stateSub.assertNoValues()
    stateSub.assertNotTerminated()
  }
}
