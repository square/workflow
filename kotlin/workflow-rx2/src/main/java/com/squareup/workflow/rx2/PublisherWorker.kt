package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.reactivestreams.Publisher

/**
 * An convenience implementation of [Worker] that is expressed in terms of Reactive Streams'
 * [Publisher] instead of [Flow].
 *
 * If you're using RxJava, [Flowable] is a [Publisher].
 *
 * Subclassing this is equivalent to just implementing [Worker.run] directly and calling [asFlow]
 * on your [Publisher].
 */
abstract class PublisherWorker<out OutputT : Any> : Worker<OutputT> {

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
  abstract fun runPublisher(): Publisher<out OutputT>

  final override fun run(): Flow<OutputT> = runPublisher().asFlow()
}
