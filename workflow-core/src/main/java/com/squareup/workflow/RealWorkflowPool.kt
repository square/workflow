/*
 * Copyright 2018 Square Inc.
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
package com.squareup.workflow

import com.squareup.workflow.WorkflowPool.Handle
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.WorkflowPool.Type
import com.squareup.workflow.WorkflowPoolMonitorEvent.Abandoned
import com.squareup.workflow.WorkflowPoolMonitorEvent.Launched
import com.squareup.workflow.WorkflowPoolMonitorEvent.ReceivedEvent
import com.squareup.workflow.WorkflowPoolMonitorEvent.Registered
import com.squareup.workflow.WorkflowPoolMonitorEvent.RemovedFromPool
import com.squareup.workflow.WorkflowPoolMonitorEvent.UpdateRequested
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.consume

internal class RealWorkflowPool(private val monitor: WorkflowPoolMonitor?) : WorkflowPool {

  private class WorkflowEntry(val workflow: Workflow<*, *, *>)

  private val launchers = mutableMapOf<Type<*, *, *>, Launcher<*, *, *>>()
  private val workflows = mutableMapOf<Id<*, *, *>, WorkflowEntry>()

  override val peekWorkflowsCount get() = workflows.values.size

  override fun <S : Any, E : Any, O : Any> register(
    launcher: Launcher<S, E, O>,
    type: Type<S, E, O>
  ) {
    launchers[type] = launcher
    monitor.report { Registered(type, launcher) }
  }

  override suspend fun <S : Any, E : Any, O : Any> awaitWorkflowUpdate(
    handle: Handle<S, E, O>
  ): WorkflowUpdate<S, E, O> {
    monitor.report { UpdateRequested(handle.id, handle.state) }
    val workflow = requireWorkflow(handle)
    workflow.openSubscriptionToState()
        .consume {
          removeCompletedWorkflowAfter(handle.id) {
            var state = receiveOrNull()
            // Skip all the states that match the handle's state.
            while (state == handle.state) {
              state = receiveOrNull()
            }

            return if (state != null) {
              monitor.report { WorkflowPoolMonitorEvent.StateChanged(handle.id, state) }
              Running(handle.copy(state = state))
            } else {
              val result = workflow.await()
              // TODO finished should be reported outside of this method, ideally.
              monitor.report { WorkflowPoolMonitorEvent.Finished(handle.id, result) }
              Finished(result)
            }
          }
        }
  }

  override suspend fun <I : Any, O : Any> awaitWorkerResult(
    worker: Worker<I, O>,
    input: I,
    name: String,
    type: Type<I, Nothing, O>
  ): O {
    val handle = Handle(type.makeWorkflowId(name), input)
    monitor.report { UpdateRequested(handle.id, handle.state) }

    register(worker.asLauncher(), type)
    val workflow = requireWorkflow(handle)

    removeCompletedWorkflowAfter(handle.id) {
      val result = workflow.await()
      monitor.report { WorkflowPoolMonitorEvent.Finished(handle.id, result) }
      return result
    }
  }

  override fun <E : Any> input(
    handle: Handle<*, E, *>
  ): WorkflowInput<E> = object : WorkflowInput<E> {
    override fun sendEvent(event: E) {
      workflows[handle.id]?.let {
        monitor.report { ReceivedEvent(handle.id, event) }
        @Suppress("UNCHECKED_CAST")
        (it.workflow as WorkflowInput<E>).sendEvent(event)
      }
    }
  }

  override fun abandonWorkflow(id: Id<*, *, *>) {
    workflows[id]?.let {
      monitor.report { Abandoned(id) }
      it.workflow.cancel()
    }
  }

  override fun abandonAll() {
    workflows.forEach { (id, entry) ->
      monitor.report { Abandoned(id) }
      entry.workflow.cancel()
    }
  }

  private fun <S : Any, E : Any, O : Any> launcher(
    type: Type<S, E, O>
  ): Launcher<S, E, O> {
    val launcher = launchers[type]
    check(launcher != null) {
      "Expected launcher for \"$type\". Did you forget to call WorkflowPool.register()?"
    }
    @Suppress("UNCHECKED_CAST")
    return launcher as Launcher<S, E, O>
  }

  private fun <S : Any, E : Any, O : Any> requireWorkflow(
    handle: Handle<S, E, O>
  ): Workflow<S, E, O> {
    // Some complexity here to handle workflows that complete the moment
    // they are started. We want to return the short-lived workflow so that its
    // result can be processed, but we also need to make sure it doesn't linger
    // in the map.

    @Suppress("UNCHECKED_CAST")
    var workflow = workflows[handle.id]?.workflow as Workflow<S, E, O>?

    if (workflow == null) {
      workflow = launcher(handle.id.workflowType).launch(handle.state, this)

      val entry = WorkflowEntry(workflow)
      // This entry will eventually be removed from the map by removeCompletedWorkflowAfter.
      workflows[handle.id] = entry

      monitor.report { Launched(handle.id, handle.state) }
    }
    return workflow
  }

  /**
   * Ensures that the workflow identified by [id] is removed from the pool map before returning,
   * iff that workflow is completed after [block] returns or throws.
   *
   * **This function must be used any time a workflow result is about to be reported.**
   */
  private inline fun <R : Any> removeCompletedWorkflowAfter(
    id: Id<*, *, *>,
    block: () -> R
  ): R = try {
    block()
  } finally {
    workflows[id]?.let { workflow ->
      if (workflow.workflow.isCompleted) {
        workflows -= id
        monitor.report { RemovedFromPool(id) }
      }
    }
  }

  private inline fun WorkflowPoolMonitor?.report(eventProvider: () -> WorkflowPoolMonitorEvent) =
    report(this@RealWorkflowPool, eventProvider)
}

private fun <I : Any, O : Any> Worker<I, O>.asLauncher() = object : Launcher<I, Nothing, O> {
  override fun launch(
    initialState: I,
    workflows: WorkflowPool
  ): Workflow<@UnsafeVariance I, Nothing, O> =
    GlobalScope.workflow(Dispatchers.Unconfined) { _, _ ->
      call(initialState)
    }
}
