@file:JvmName("WorkflowPools")

package com.squareup.workflow.rx2

import com.squareup.workflow.Delegating
import com.squareup.workflow.Reaction
import com.squareup.workflow.WorkflowPool
import io.reactivex.Single
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.rx2.asSingle
import com.squareup.workflow.nextDelegateReaction as nextDelegateReactionCore

/**
 * Starts the required nested workflow if it wasn't already running. Returns
 * a [Single] that will fire the next time the nested workflow updates its state,
 * or completes.
 *
 * If the nested workflow was not already running, it is started in the
 * [given state][Delegating.delegateState], and that state is reported by the
 * returned [Single]. Otherwise, the [Single] skips state updates that match the given
 * state.
 *
 * If the nested workflow is [abandoned][WorkflowPool.abandonDelegate], the [Single] never
 * completes.
 */
fun <S : Any, O : Any> WorkflowPool.nextDelegateReaction(
  delegating: Delegating<S, *, O>
): Single<Reaction<S, O>> = nextDelegateReactionCore(delegating).asSingle(Unconfined)
    .onErrorResumeNext {
      if (it is CancellationException) {
        Single.never()
      } else {
        Single.error(it)
      }
    }
