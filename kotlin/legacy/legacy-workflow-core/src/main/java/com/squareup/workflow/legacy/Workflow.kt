/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Models a process in the app as a stream of [states][openSubscriptionToState] of type [S],
 * followed by a [result][await] of type [O] when the process is done.
 *
 * A `Workflow` can be cancelled by calling [cancel].
 */
@Deprecated("Use com.squareup.workflow.Workflow")
interface Workflow<out S : Any, in E : Any, out O : Any> : Deferred<O>, WorkflowInput<E> {
  /**
   * Returns a channel that will, on every update, report the complete, current state of this
   * workflow.
   *
   * The channel must be canceled to unsubscribe from the stream.
   *
   * Note that, unlike RxJava-style `Observable`s, the returned [ReceiveChannel]s are not
   * multicasting – multiple consumers reading from the same channel will only see distinct values.
   * Each consumer must get its own channel by calling this method.
   *
   * The channel will throw a [CancellationException] if the workflow is abandoned. This method
   * itself will not throw.
   */
  fun openSubscriptionToState(): ReceiveChannel<S>
}
