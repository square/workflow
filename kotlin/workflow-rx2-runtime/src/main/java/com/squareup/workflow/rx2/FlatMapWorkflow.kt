/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow.rx2

import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHost.Update
import com.squareup.workflow.runWorkflowTree
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.reactive.openSubscription
import kotlinx.coroutines.rx2.rxFlowable

/**
 * Given a stream of [InputT] values to use as inputs for the top-level [workflow], returns a
 * [Flowable] that, when subscribed to, will start [workflow] and emit its [Update]s. When the
 * subscription is disposed, the workflow will be terminated.
 *
 * The returned [Flowable] is _not_ multicasted – **every subscription will start a new workflow
 * session.** It is recommended to use a multicasting operator on the resulting stream, such as
 * [Flowable.replay], to share the updates from a single workflow session.
 *
 * The workflow's logic will run in whatever threading context the source [Flowable] is being
 * observed on.
 *
 * This operator is an alternative to using
 * [WorkflowHost.Factory][com.squareup.workflow.WorkflowHost.Factory] that is more convenient to
 * use with a stream of inputs.
 *
 * This function operates on [Flowable] instead of [Observable] because the workflow runtime
 * inherently supports backpressure. [Flowable] supports backpressure, [Observable] does not.
 * RxJava provides operators to adapt between [Flowable]s and [Observable]s by explicitly specifying
 * how to use backpressure. By operating on [Flowable], this operator leaves it up to the caller to
 * specify strategies for handling backpressure, instead of assuming any particular behavior.
 */
fun <InputT, OutputT : Any, RenderingT> Flowable<InputT>.flatMapWorkflow(
  workflow: Workflow<InputT, OutputT, RenderingT>,
  initialSnapshot: Snapshot? = null,
  dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
): Flowable<Update<OutputT, RenderingT>> =
// We're ok not having a job here because the lifetime of the coroutine will be controlled by the
  // subscription to the resulting flowable.
  GlobalScope.rxFlowable(context = dispatcher) {
    // Convert the input stream into a channel.
    openSubscription().consume {
      runWorkflowTree(
          workflow = workflow.asStatefulWorkflow(),
          inputs = this,
          initialSnapshot = initialSnapshot,
          onUpdate = ::send
      )
    }
  }
