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
package com.squareup.workflow.internal

import com.squareup.workflow.Worker
import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Worker.OutputOrFinished.Finished
import com.squareup.workflow.Worker.OutputOrFinished.Output
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.SelectBuilder

/**
 * Launches a new coroutine that is a child of this node's scope, and calls
 * [com.squareup.workflow.Worker.run] from that coroutine. Returns a [ReceiveChannel] that
 * will emit everything from the worker. The channel will be closed when the flow completes.
 */
@UseExperimental(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun <T> CoroutineScope.launchWorker(worker: Worker<T>): ReceiveChannel<Output<T>> =
  worker.run()
      // TODO(https://github.com/square/workflow/issues/434) Remove this map to allow operator
      // fusion to occur.
      .map { Output(it) }
      .produceIn(this)

/**
 * Wraps [ReceiveChannel.onReceiveOrNull] to detect if the channel is actually closed vs just
 * emitting a null value. Once `receiveOrClosed` support lands in the coroutines library, we should
 * use that instead.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
internal inline fun <T, R> SelectBuilder<R>.onReceiveOutputOrFinished(
  channel: ReceiveChannel<Output<T>>,
  crossinline handler: (OutputOrFinished<T>) -> R
) {
  channel.onReceiveOrNull { maybeOutput -> handler(maybeOutput ?: Finished) }
}
