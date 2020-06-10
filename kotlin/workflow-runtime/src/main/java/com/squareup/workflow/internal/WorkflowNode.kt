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
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.applyTo
import com.squareup.workflow.diagnostic.IdCounter
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.diagnostic.createId
import com.squareup.workflow.internal.RealRenderContext.WorkerRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.selects.SelectBuilder
import okio.ByteString
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A node in a state machine tree. Manages the actual state for a given [Workflow].
 *
 * @param emitOutputToParent A function that this node will call when it needs to emit an output
 * value to its parent. Returns either the output to be emitted from the root workflow, or null.
 * @param initialState Allows unit tests to start the node from a given state, instead of calling
 * [StatefulWorkflow.initialState].
 * @param workerContext [CoroutineContext] that is appended to the end of the context used to launch
 * worker coroutines. This context will override anything from the workflow's scope and any other
 * hard-coded values added to worker contexts. It must not contain a [Job] element (it would violate
 * structured concurrency).
 */
@OptIn(VeryExperimentalWorkflow::class)
internal class WorkflowNode<PropsT, StateT, OutputT : Any, RenderingT>(
  val id: WorkflowId<PropsT, OutputT, RenderingT>,
  workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
  initialProps: PropsT,
  snapshot: ByteString?,
  baseContext: CoroutineContext,
  private val emitOutputToParent: (OutputT) -> Any? = { it },
  parentDiagnosticId: Long? = null,
  private val diagnosticListener: WorkflowDiagnosticListener? = null,
  private val idCounter: IdCounter? = null,
  initialState: StateT? = null,
  private val workerContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScope, WorkerRunner<StateT, OutputT> {

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

  private val subtreeManager = SubtreeManager<StateT, OutputT>(
      coroutineContext, ::applyAction, diagnosticId, diagnosticListener, idCounter, workerContext
  )

  private val workers = ActiveStagingList<WorkerChildNode<*, *, *>>()

  private var state: StateT

  private var lastProps: PropsT = initialProps

  private val eventActionsChannel = Channel<WorkflowAction<StateT, OutputT>>(capacity = UNLIMITED)

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
    @Suppress("UNCHECKED_CAST")
    val typedWorkflow = workflow as StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>
    val childSnapshots = subtreeManager.createChildSnapshots()
    return createTreeSnapshot(
        rootSnapshot = typedWorkflow.snapshotState(state),
        childSnapshots = childSnapshots
    )
  }

  override fun <T> runningWorker(
    worker: Worker<T>,
    key: String,
    handler: (T) -> WorkflowAction<StateT, OutputT>
  ) {
    // Prevent duplicate workflows with the same key.
    workers.forEachStaging {
      require(!(it.matches(worker, key))) {
        "Expected keys to be unique for $worker: key=$key"
      }
    }

    // Start tracking this case so we can be ready to render it.
    val stagedWorker = workers.retainOrCreate(
        predicate = { it.matches(worker, key) },
        create = { createWorkerNode(worker, key, handler) }
    )
    stagedWorker.setHandler(handler)
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
  fun <T : Any> tick(selector: SelectBuilder<T?>) {
    // Listen for any child workflow updates.
    subtreeManager.tickChildren(selector)

    // Listen for any subscription updates.
    workers.forEachActive { child ->
      // Skip children that have finished but are still being run by the workflow.
      if (child.tombstone) return@forEachActive

      with(selector) {
        child.channel.onReceive { valueOrDone ->
          if (valueOrDone.isDone) {
            // Set the tombstone flag so we don't continue to listen to the subscription.
            child.tombstone = true
            // Nothing to do on close other than update the session, so don't emit any output.
            return@onReceive null
          } else {
            val update = child.acceptUpdate(valueOrDone.value)
            @Suppress("UNCHECKED_CAST")
            return@onReceive applyAction(update as WorkflowAction<StateT, OutputT>)
          }
        }
      }
    }

    // Listen for any events.
    with(selector) {
      eventActionsChannel.onReceive { action ->
        diagnosticListener?.onSinkReceived(diagnosticId, action)
        return@onReceive applyAction(action)
      }
    }
  }

  /**
   * Cancels this state machine host, and any coroutines started as children of it.
   *
   * This must be called when the caller will no longer call [tick]. It is an error to call [tick]
   * after calling this method.
   */
  fun cancel(cause: CancellationException? = null) {
    // No other cleanup work should be done in this function, since it will only be invoked when
    // this workflow is *directly* discarded by its parent (or the host).
    // If you need to do something whenever this workflow is torn down, add it to the
    // invokeOnCompletion handler for the Job above.
    coroutineContext.cancel(cause)
  }

  /**
   * Contains the actual logic for [render], after we've casted the passed-in [Workflow]'s
   * state type to our `StateT`.
   */
  private fun renderWithStateType(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    props: PropsT
  ): RenderingT {
    updatePropsAndState(workflow, props)

    val context = RealRenderContext(
        renderer = subtreeManager,
        workerRunner = this,
        eventActionsChannel = eventActionsChannel
    )
    diagnosticListener?.onBeforeWorkflowRendered(diagnosticId, props, state)
    val rendering = workflow.render(props, state, context)
    context.freeze()
    diagnosticListener?.onAfterWorkflowRendered(diagnosticId, rendering)

    // Tear down workflows and workers that are obsolete.
    subtreeManager.commitRenderedChildren()
    workers.commitStaging { it.channel.cancel() }

    return rendering
  }

  private fun updatePropsAndState(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    newProps: PropsT
  ) {
    if (newProps != lastProps) {
      val newState = workflow.onPropsChanged(lastProps, newProps, state)
      diagnosticListener?.onPropsChanged(diagnosticId, lastProps, newProps, state, newState)
      state = newState
    }
    lastProps = newProps
  }

  /**
   * Applies [action] to this workflow's [state] and
   * [emits an output to its parent][emitOutputToParent] if necessary.
   */
  private fun <T : Any> applyAction(action: WorkflowAction<StateT, OutputT>): T? {
    val (newState, output) = action.applyTo(state)
    diagnosticListener?.onWorkflowAction(diagnosticId, action, state, newState, output)
    state = newState
    @Suppress("UNCHECKED_CAST")
    return output?.let(emitOutputToParent) as T?
  }

  private fun <T> createWorkerNode(
    worker: Worker<T>,
    key: String,
    handler: (T) -> WorkflowAction<StateT, OutputT>
  ): WorkerChildNode<T, StateT, OutputT> {
    var workerId = 0L
    if (diagnosticListener != null) {
      workerId = idCounter.createId()
      diagnosticListener.onWorkerStarted(workerId, diagnosticId, key, worker.toString())
    }
    val workerChannel =
      launchWorker(worker, key, workerId, diagnosticId, diagnosticListener, workerContext)
    return WorkerChildNode(worker, key, workerChannel, handler = handler)
  }

  private fun ByteString.restoreState(): Snapshot? {
    val (snapshotToRestoreFrom, childSnapshots) = parseTreeSnapshot(this)
    subtreeManager.restoreChildrenFromSnapshots(childSnapshots)
    return snapshotToRestoreFrom?.let { Snapshot.of(it) }
  }
}
