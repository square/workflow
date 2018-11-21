package com.squareup.workflow.rx2

import com.squareup.workflow.WorkflowInput
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable

/**
 * Models a process in the app as a stream of [states][state] of type [S], followed by
 * a [result] of type [O] when the process is done.
 */
interface Workflow<out S : Any, in E : Any, out O : Any> : WorkflowInput<E> {
  /**
   * On every update, reports the complete, current state of this workflow.
   */
  val state: Observable<out S>

  /**
   * Called with the final result of this workflow. The result value should be cached – that is,
   * the result should be stored and emitted even if the [Maybe] isn't subscribed to until
   * after the workflow completes. It should also support multiple subscribers.
   *
   * If the workflow is abandoned, the result will complete without a value.
   */
  val result: Maybe<out O>

  /**
   * May be called to advise a running workflow that it is to be abandoned prematurely, before
   * it has fired its [result]. Provided as an optional hook to allow implementations to handle any
   * tear-down concerns.
   *
   * This method has a default no-op implementation, so if your workflow is written in Kotlin and
   * you don't care about this hook, you needn't override this method.
   */
  fun abandon()
}

/**
 * Returns a [Completable] that fires either when [Workflow.result] fires, or when
 * [Workflow.abandon] is called.
 */
fun Workflow<*, *, *>.toCompletable(): Completable = state.ignoreElements()

/**
 * [Transforms][https://stackoverflow.com/questions/15457015/explain-contramap]
 * the receiver to accept events of type [E2] instead of [E1].
 */
fun <S : Any, E2 : Any, E1 : Any, O : Any> Workflow<S, E1, O>.adaptEvents(transform: (E2) -> E1):
    Workflow<S, E2, O> {
  return object : Workflow<S, E2, O> {
    override fun sendEvent(event: E2) = this@adaptEvents.sendEvent(transform(event))
    override val state: Observable<out S> = this@adaptEvents.state
    override val result = this@adaptEvents.result
    override fun abandon() = this@adaptEvents.abandon()
  }
}

/**
 * Transforms the receiver to emit states of type [S2] instead of [S1].
 */
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.mapState(transform: (S1) -> S2):
    Workflow<S2, E, O> {
  return object : Workflow<S2, E, O> {
    override val state: Observable<out S2> = this@mapState.state.map(transform)
    override fun sendEvent(event: E) = this@mapState.sendEvent(event)
    override val result = this@mapState.result
    override fun abandon() = this@mapState.abandon()
  }
}

/**
 * Like [mapState], transforms the receiving workflow with [Workflow.state] of type
 * [S1] to one with states of [S2]. Unlike that method, each [S1] update is transformed
 * into a stream of [S2] updates -- useful when an [S1] state might wrap an underlying
 * workflow whose own screens need to be shown.
 */
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.switchMapState(
  transform: (S1) -> Observable<out S2>
): Workflow<S2, E, O> {
  return object : Workflow<S2, E, O> {
    override val state: Observable<out S2> = this@switchMapState.state.switchMap(transform)
    override fun sendEvent(event: E) = this@switchMapState.sendEvent(event)
    override val result = this@switchMapState.result
    override fun abandon() = this@switchMapState.abandon()
  }
}

/**
 * Transforms the receiver to emit a result of type [O2] instead of [O1].
 */
fun <S : Any, E : Any, O1 : Any, O2 : Any> Workflow<S, E, O1>.mapResult(transform: (O1) -> O2):
    Workflow<S, E, O2> {
  return object : Workflow<S, E, O2> {
    override val state: Observable<out S> = this@mapResult.state
    override fun sendEvent(event: E) = this@mapResult.sendEvent(event)
    override val result = this@mapResult.result.map(transform)
    override fun abandon() = this@mapResult.abandon()
  }
}
