package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import com.squareup.workflow.worker
import io.reactivex.Single
import kotlinx.coroutines.experimental.rx2.await

/**
 * Creates a [Worker] that will pass its input value to [block], then subscribe to the returned
 * [Single] and report the value it emits as the worker result.
 */
fun <I : Any, O : Any> singleWorker(block: (I) -> Single<O>): Worker<I, O> =
  worker { block(it).await() }

/**
 * Creates a [Worker] that will report the [Single]'s eventual value as its result.
 */
fun <T : Any> Single<T>.asWorker(): Worker<Unit, T> = worker { await() }
