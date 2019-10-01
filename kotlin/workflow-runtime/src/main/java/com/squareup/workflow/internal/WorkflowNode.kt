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
import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.applyTo
import com.squareup.workflow.diagnostic.IdCounter
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.diagnostic.createId
import com.squareup.workflow.internal.Behavior.WorkerCase
import com.squareup.workflow.parse
import com.squareup.workflow.readByteStringWithLength
import com.squareup.workflow.writeByteStringWithLength
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.SelectBuilder
import okio.ByteString
import kotlin.coroutines.CoroutineContext

/**
 * A node in a state machine tree. Manages the actual state for a given [Workflow].
 *
 * @param initialState Allows unit tests to start the node from a given state, instead of calling
 * [StatefulWorkflow.initialState].
 */
@UseExperimental(VeryExperimentalWorkflow::class)
internal class WorkflowNode<PropsT, StateT, OutputT : Any, RenderingT>(
  val id: WorkflowId<PropsT, OutputT, RenderingT>,
  workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
  initialProps: PropsT,
  snapshot: ByteString?,
  baseContext: CoroutineContext,
  parentDiagnosticId: Long? = null,
  private val diagnosticListener: WorkflowDiagnosticListener? = null,
  private val idCounter: IdCounter? = null,
  initialState: StateT? = null
) : CoroutineScope {

  /**
   * Holds the channel representing the outputs of a worker, as well as a tombstone flag that is
   * true after the worker has finished and we've reported that fact to the workflow. This is to
   * prevent the workflow from entering an infinite loop of getting `Finished` events if it
   * continues to listen to the worker after it finishes.
   */
  private class WorkerSession(
    val channel: ReceiveChannel<*>,
    var tombstone: Boolean = false
  )

  /**
   * Context that has a job that will live as long as this node.
   * Also adds a debug name to this coroutine based on its ID.
   */
  override val coroutineContext = baseContext + Job(baseContext[Job]) + CoroutineName(id.toString())

  /**
   * ID used to uniquely identify this node to [WorkflowDiagnosticListener]s.
   *
   * Visible for testing.
   */
  internal val diagnosticId = idCounter.createId()

  private val subtreeManager =
    SubtreeManager<StateT, OutputT>(coroutineContext, diagnosticId, diagnosticListener, idCounter)

  private val workerTracker =
    LifetimeTracker<WorkerCase<*, StateT, OutputT>, Any, WorkerSession>(
        getKey = { case -> case },
        start = { case ->
          var workerId = 0L
          if (diagnosticListener != null) {
            workerId = idCounter.createId()
            diagnosticListener
                .onWorkerStarted(workerId, diagnosticId, case.key, case.worker.toString())
          }
          val workerChannel = launchWorker(case.worker, workerId, diagnosticId, diagnosticListener)
          WorkerSession(workerChannel)
        },
        dispose = { _, session -> session.channel.cancel() }
    )

  private var state: StateT

  private var lastProps: PropsT = initialProps

  private var behavior: Behavior<StateT, OutputT>? = null

  init {
    var restoredFromSnapshot = false
    state = if (initialState != null) {
      initialState
    } else {
      val snapshotToRestoreFrom = snapshot?.restoreState()
      if (snapshotToRestoreFrom != null) {
        restoredFromSnapshot = true
      }
      workflow.initialState(initialProps, snapshotToRestoreFrom)
    }

    if (diagnosticListener != null) {
      diagnosticListener.onWorkflowStarted(
          diagnosticId, parentDiagnosticId, id.typeDebugString, id.name, initialProps, state,
          restoredFromSnapshot
      )
      coroutineContext[Job]!!.invokeOnCompletion {
        diagnosticListener.onWorkflowStopped(diagnosticId)
      }
    }
  }

  /**
   * Walk the tree of workflows, rendering each one and using
   * [RenderContext][com.squareup.workflow.RenderContext] to give its children a chance to
   * render themselves and aggregate those child renderings.
   */
  @Suppress("UNCHECKED_CAST")
  fun render(
    workflow: StatefulWorkflow<PropsT, *, OutputT, RenderingT>,
    input: PropsT
  ): RenderingT =
    renderWithStateType(workflow as StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>, input)

  /**
   * Walk the tree of state machines again, this time gathering snapshots and aggregating them
   * automatically.
   */
  fun snapshot(workflow: StatefulWorkflow<*, *, *, *>): Snapshot {
    val childrenSnapshot = subtreeManager.createChildrenSnapshot()
    @Suppress("UNCHECKED_CAST")
    return childrenSnapshot.withState(
        workflow as StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>
    )
  }

  /**
   * Gets the next [output][OutputT] from the state machine.
   *
   * Walk the tree of state machines, asking each one to wait for its next event. If something happen
   * that results in an output, that output is returned. Null means something happened that requires
   * a re-render, e.g. my state changed or a child state changed.
   *
   * It is an error to call this method after calling [cancel].
   */
  @UseExperimental(InternalCoroutinesApi::class)
  fun <T : Any> tick(
    selector: SelectBuilder<T?>,
    handler: (OutputT) -> T?
  ) {
    fun acceptUpdate(action: WorkflowAction<StateT, OutputT>): T? {
      val (newState, output) = action.applyTo(state)
      diagnosticListener?.onWorkflowAction(diagnosticId, action, state, newState, output)
      state = newState
      return output?.let(handler)
    }

    // Listen for any child workflow updates.
    subtreeManager.tickChildren(selector, ::acceptUpdate)

    // Listen for any subscription updates.
    workerTracker.lifetimes
        .filter { (_, session) -> !session.tombstone }
        .forEach { (case, session) ->
          with(selector) {
            session.channel.onReceiveOrClosed { valueOrClosed ->
              if (valueOrClosed.isClosed) {
                // Set the tombstone flag so we don't continue to listen to the subscription.
                session.tombstone = true
                // Nothing to do on close other than update the session, so don't emit any output.
                return@onReceiveOrClosed null
              } else {
                val update = case.acceptUpdate(valueOrClosed.value)
                acceptUpdate(update)
              }
            }
          }
        }

    // Listen for any events.
    with(selector) {
      behavior!!.nextActionFromEvent.onAwait { action ->
        diagnosticListener?.onSinkReceived(diagnosticId, action)
        acceptUpdate(action)
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
   * Contains the actual logic for [render], after we've casted the passed-in [Workflow]'s
   * state type to our `StateT`.
   */
  private fun renderWithStateType(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    input: PropsT
  ): RenderingT {
    updatePropsAndState(workflow, input)

    val context = RealRenderContext(subtreeManager)
    diagnosticListener?.onBeforeWorkflowRendered(diagnosticId, input, state)
    val rendering = workflow.render(input, state, context)
    diagnosticListener?.onAfterWorkflowRendered(diagnosticId, rendering)

    behavior = context.buildBehavior()
        .apply {
          // Start new children/workers, and drop old ones.
          subtreeManager.track(childCases)
          workerTracker.track(workerCases)
        }

    return rendering
  }

  private fun updatePropsAndState(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    newProps: PropsT
  ) {
    val newState = workflow.onPropsChanged(lastProps, newProps, state)
    diagnosticListener?.onPropsChanged(diagnosticId, lastProps, newProps, state, newState)
    state = newState
    lastProps = newProps
  }

  /** @see ByteString.parsePartialSnapshot */
  private fun Snapshot.withState(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>
  ): Snapshot {
    val stateSnapshot = workflow.snapshotState(state)
    return Snapshot.write { sink ->
      sink.writeByteStringWithLength(stateSnapshot.bytes)
      sink.write(bytes)
    }
  }

  private fun ByteString.restoreState(): Snapshot? {
    val (snapshotToRestoreFrom, childrenSnapshot) = parsePartialSnapshot()
    subtreeManager.restoreChildrenFromSnapshot(childrenSnapshot)
    return snapshotToRestoreFrom
  }

  /** @see Snapshot.withState */
  private fun ByteString.parsePartialSnapshot(): Pair<Snapshot?, ByteString> = parse { source ->
    val stateSnapshot = source.readByteStringWithLength()
    val childrenSnapshot = source.readByteString()
    // Never pass an empty snapshot to initialState.
    val nonEmptySnapshot = stateSnapshot.takeIf { it.size > 0 }
        ?.let { Snapshot.of(it) }
    return Pair(nonEmptySnapshot, childrenSnapshot)
  }
}
