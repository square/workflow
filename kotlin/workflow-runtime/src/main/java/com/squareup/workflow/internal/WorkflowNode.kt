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

import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.internal.Behavior.SubscriptionCase
import com.squareup.workflow.parse
import com.squareup.workflow.readByteStringWithLength
import com.squareup.workflow.util.ChannelUpdate.Closed
import com.squareup.workflow.writeByteStringWithLength
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.SelectBuilder
import kotlin.coroutines.CoroutineContext

/**
 * A node in a state machine tree. Manages the actual state for a given [Workflow].
 *
 * @param initialState Allows unit tests to start the node from a given state, instead of calling
 * [StatefulWorkflow.initialState].
 */
internal class WorkflowNode<InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any>(
  val id: WorkflowId<InputT, OutputT, RenderingT>,
  workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
  initialInput: InputT,
  snapshot: Snapshot?,
  baseContext: CoroutineContext,
  initialState: StateT? = null
) : CoroutineScope {

  /**
   * Tracks a [ReceiveChannel] that represents a "subscription" to an observable data source, and
   * a [flag][tombstone] that indicates whether the data source has reported that it's been closed
   * to the workflow.
   *
   * The tombstone flag is `false` when the source is "closed" (channel closed, observable complete,
   * etc.) and then it is set to `true` so that the close will not be delivered again.
   *
   * When [tombstone] is `true`, the subscription will never deliver any more updates to the workflow.
   *
   * Terminal subscriptions are kept around so that they won't be unsubscribed and resubscribed to
   * on the next render pass.
   */
  private class Subscription(
    val channel: ReceiveChannel<*>,
    var tombstone: Boolean = false
  )

  /**
   * Context that has a job that will live as long as this node.
   * Also adds a debug name to this coroutine based on its ID.
   */
  override val coroutineContext = baseContext + Job(baseContext[Job]) + CoroutineName(id.toString())

  init {
    // This will get invoked whenever we are cancelled (via our cancel() method), or whenever
    // any of our ancestor workflows are cancelled, anywhere up the tree, including whenever an
    // exception is thrown that cancels a workflow.
    coroutineContext[Job]!!.invokeOnCompletion {
      behavior?.teardownHooks?.forEach { it.invoke() }
    }
  }

  private val subtreeManager = SubtreeManager<StateT, OutputT>(coroutineContext)
  private val subscriptionTracker =
    LifetimeTracker<SubscriptionCase<*, StateT, OutputT>, Any, Subscription>(
        getKey = { case -> case.idempotenceKey },
        start = { case -> Subscription(case.channelProvider.invoke(this)) },
        dispose = { _, subscription -> subscription.channel.cancel() }
    )

  private var state: StateT = initialState
      ?: snapshot?.restoreState(initialInput, workflow)
      ?: workflow.initialState(initialInput, snapshot = null)

  private var lastInput: InputT = initialInput

  private var behavior: Behavior<StateT, OutputT>? = null

  /**
   * Walk the tree of workflows, rendering each one and using
   * [WorkflowContext][com.squareup.workflow.WorkflowContext] to give its children a chance to
   * compose themselves and aggregate those child renderings.
   */
  @Suppress("UNCHECKED_CAST")
  fun compose(
    workflow: StatefulWorkflow<InputT, *, OutputT, RenderingT>,
    input: InputT
  ): RenderingT =
    composeWithStateType(workflow as StatefulWorkflow<InputT, StateT, OutputT, RenderingT>, input)

  /**
   * Walk the tree of state machines again, this time gathering snapshots and aggregating them
   * automatically.
   */
  fun snapshot(workflow: StatefulWorkflow<*, *, *, *>): Snapshot {
    val childrenSnapshot = subtreeManager.createChildrenSnapshot()
    @Suppress("UNCHECKED_CAST")
    return childrenSnapshot.withState(
        workflow as StatefulWorkflow<InputT, StateT, OutputT, RenderingT>
    )
  }

  /**
   * Gets the next [output][OutputT] from the state machine.
   *
   * Walk the tree of state machines, asking each one to wait for its next event. If something happen
   * that results in an output, that output is returned. Null means something happened that requires
   * a re-compose, e.g. my state changed or a child state changed.
   *
   * It is an error to call this method after calling [cancel].
   */
  fun <T : Any> tick(
    selector: SelectBuilder<T?>,
    handler: (OutputT) -> T?
  ) {
    fun acceptUpdate(action: WorkflowAction<StateT, OutputT>): T? {
      val (newState, output) = action(state)
      state = newState
      return output?.let(handler)
    }

    // Listen for any child workflow updates.
    subtreeManager.tickChildren(selector, ::acceptUpdate)

    // Listen for any subscription updates.
    subscriptionTracker.lifetimes
        .filter { (_, sub) -> !sub.tombstone }
        .forEach { (case, subscription) ->
          selector.onChannelUpdate(subscription.channel) { channelUpdate ->
            if (channelUpdate === Closed) {
              // Set the tombstone flag so we don't continue to listen to the subscription.
              subscription.tombstone = true
            }
            val update = case.acceptUpdate(channelUpdate)
            acceptUpdate(update)
          }
        }

    // Listen for any events.
    with(selector) {
      behavior!!.nextActionFromEvent.onAwait { update ->
        acceptUpdate(update)
      }
    }
  }

  /**
   * Cancels this state machine host, and any coroutines started as children of it.
   *
   * This must be called when the caller will no longer call [tick]. It is an error to call [tick]
   * after calling this method.
   */
  fun cancel() {
    // No other cleanup work should be done in this function, since it will only be invoked when
    // this workflow is *directly* discarded by its parent (or the host).
    // If you need to do something whenever this workflow is torn down, add it to the
    // invokeOnCompletion handler for the Job above.
    coroutineContext.cancel()
  }

  /**
   * Contains the actual logic for [compose], after we've casted the passed-in [Workflow]'s
   * state type to our `StateT`.
   */
  private fun composeWithStateType(
    workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
    input: InputT
  ): RenderingT {
    updateInputAndState(workflow, input)

    val context = RealWorkflowContext(subtreeManager)
    val rendering = workflow.compose(input, state, context)

    behavior = context.buildBehavior()
        .apply {
          // Start new children/subscriptions, and drop old ones.
          subtreeManager.track(childCases)
          subscriptionTracker.track(subscriptionCases)
        }

    return rendering
  }

  private fun updateInputAndState(
    workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
    newInput: InputT
  ) {
    state = workflow.onInputChanged(lastInput, newInput, state)
    lastInput = newInput
  }

  /** @see Snapshot.parsePartial */
  private fun Snapshot.withState(
    workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>
  ): Snapshot {
    val stateSnapshot = workflow.snapshotState(state)
    return Snapshot.write { sink ->
      sink.writeByteStringWithLength(stateSnapshot.bytes)
      sink.write(bytes)
    }
  }

  private fun Snapshot.restoreState(
    input: InputT,
    workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>
  ): StateT {
    val (state, childrenSnapshot) = parsePartial(input, workflow)
    subtreeManager.restoreChildrenFromSnapshot(childrenSnapshot)
    return state
  }

  /** @see Snapshot.withState */
  private fun Snapshot.parsePartial(
    input: InputT,
    workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>
  ): Pair<StateT, Snapshot> =
    bytes.parse { source ->
      val stateSnapshot = source.readByteStringWithLength()
      val childrenSnapshot = source.readByteString()
      val state = workflow.initialState(input, Snapshot.of(stateSnapshot))
      return Pair(state, Snapshot.of(childrenSnapshot))
    }
}
