package com.squareup.reactor.rx2

import com.squareup.reactor.CoroutineReactor
import com.squareup.reactor.EnterState
import com.squareup.reactor.FinishWith
import com.squareup.reactor.Reaction
import com.squareup.reactor.ReactorException
import com.squareup.workflow.Rx2Workflow
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.rx2.rxSingle
import kotlin.coroutines.experimental.CoroutineContext

internal class ReactorRx2Workflow<S : Any, in E : Any, out O : Any>(
  private val reactor: CoroutineReactor<S, E, O>,
  private val initialState: S
) : Rx2Workflow<S, E, O>, CoroutineScope {

  // The events channel is buffered primarily so that reactors don't race themselves when state
  // changes immediately cause more events to be sent.
  private val events = Channel<E>(Channel.UNLIMITED)

  private val stateSubject = BehaviorSubject.create<Single<Reaction<S, O>>>()

  private val _state = stateSubject.flatMapSingle { it }
      .doOnNext { currentState ->
        val previousState = currentStateForSnapshot
        check(previousState !is FinishWith<O>) {
          "Reactor has already been finished, cannot change state anymore."
        }

        currentStateForSnapshot = currentState

        if (currentState is EnterState<S>) {
          val nextState = tryReact(currentState.state)
          @Suppress("UNCHECKED_CAST")
          stateSubject.onNext(nextState as Single<Reaction<S, O>>)
        }
      }
      .replay(1)

  /** Used to disconnect from the state relay on abandonment. */
  private var stateSubscription: Disposable = Disposables.disposed()

  private var currentStateForSnapshot: Reaction<S, O>? = null

  private val isStarted: Boolean get() = stateSubject.hasValue()

  /**
   * Defines the context for the coroutine from which the [Rx2Reactor.onReact] method is called.
   */
  override val coroutineContext: CoroutineContext =
  // Unconfined means the coroutine is never dispatched and always resumed synchronously on the
  // current thread. This is the default RxJava behavior, and the behavior we explicitly want here.
    Dispatchers.Unconfined

  // The events channel has an unlimited-size buffer, this should never fail. See RF-1619.
  override fun sendEvent(event: E) = check(events.offer(event)) {
    "Expected EventChannel to accept event: $event"
  }

  @Suppress("UNCHECKED_CAST")
  override val state: Observable<out S> = _state.takeWhile { it is EnterState<S> }
      // Required to make sure we complete when abandoned.
      .takeUntil(stateSubject.ignoreElements().toObservable<Unit>())
      .ofType(EnterState::class.java as Class<EnterState<S>>)
      .map { it.state }

  @Suppress("UNCHECKED_CAST")
  override val result: Maybe<out O> = _state.ofType(FinishWith::class.java as Class<FinishWith<O>>)
      .firstElement()
      .map { it.result }
      // Close the event channel as soon as we're complete, either successfully or with an error.
      .doOnEvent { _, _ -> events.close() }
      .cache()

  override fun abandon() {
    events.close()
    stateSubscription.dispose()

    currentStateForSnapshot.let {
      if (it is EnterState<S>) {
        reactor.abandon(it.state)
      }
    }
    stateSubject.onComplete()
  }

  override fun toString(): String {
    return "${javaClass.simpleName}($reactor @ $currentStateForSnapshot)"
  }

  fun start() {
    check(!isStarted) { "Reactor has already been started" }
    stateSubject.onNext(Single.just(EnterState(initialState)))
    stateSubscription = _state.connect()
  }

  /**
   * Ensures any errors thrown either by the [Rx2Reactor.onReact] method or the [Single] it returns are
   * wrapped in a [ReactorException] and transmitted through Rx plumbing.
   */
  private fun tryReact(currentState: S): Single<out Reaction<S, O>> {
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    return rxSingle { reactor.react(currentState, events) }
        .onErrorResumeNext { cause ->
          Single.error(
              ReactorException(
                  cause = cause,
                  reactor = reactor,
                  reactorState = currentState
              )
          )
        }
  }
}
