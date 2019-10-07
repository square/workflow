package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow

/**
 * An convenience implementation of [Worker] that is expressed in terms of Rx [Flowable] instead
 * of [Flow].
 *
 * Subclassing this is equivalent to just implementing [Worker.run] directly and calling [asFlow]
 * on your [Flowable], but doesn't require you to add
 * `@UseExperimental(ExperimentalCoroutinesApi::class)` to your code.
 */
abstract class RxWorker<out OutputT : Any> : Worker<OutputT> {

  /**
   * Returns a [Flowable] to execute the work represented by this worker.
   *
   * If you have an [io.reactivex.Observable] instead, just call
   * [toFlowable][io.reactivex.Observable.toFlowable] to convert it.
   *
   * The [Flowable] is subscribed to in the context of the workflow runtime. When this [Worker],
   * its parent [Workflow], or any ancestor [Workflow]s are torn down, the subscription will be
   * [disposed][io.reactivex.disposables.Disposable.dispose].
   */
  abstract fun runRx(): Flowable<out OutputT>

  @UseExperimental(ExperimentalCoroutinesApi::class)
  final override fun run(): Flow<OutputT> = runRx().asFlow()
}
