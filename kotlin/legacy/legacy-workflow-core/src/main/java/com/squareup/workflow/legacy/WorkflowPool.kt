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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy

import com.squareup.workflow.legacy.WorkflowPool.Handle
import com.squareup.workflow.legacy.WorkflowPool.Launcher
import com.squareup.workflow.legacy.WorkflowPool.Type
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consume
import org.jetbrains.annotations.TestOnly
import kotlin.reflect.KClass

/**
 * Runs [Workflow] and [Worker]s on demand.
 *
 * To run a [Workflow]:
 *
 *  - [register] a [Launcher] of the appropriate type
 *  - create a matching [Handle] call [awaitWorkflowUpdate]
 *
 * To run a [Worker], call [awaitWorkerResult]. (A worker is effectively its own [Launcher].)
 */
@Deprecated("Use com.squareup.workflow.Workflow")
class WorkflowPool {
  /**
   * Represents the type parameters of a [Launcher] in reified form.
   *
   * To get the type of a [Launcher], use one of the following properties:
   *  - [Launcher.workflowType]
   *  - `KClass<Launcher>.workflowType`
   */
  class Type<S : Any, in E : Any, out O : Any>(
    stateType: KClass<S>,
    eventType: KClass<E>,
    outputType: KClass<O>
  ) {
    private val types = arrayOf(stateType, eventType, outputType)

    /** Only calculate this once. */
    private val hashCode = types.contentHashCode()

    /**
     * Creates an id for an instance of this type to be managed by a [WorkflowPool].
     * It is rare to create an [Id] yourself. They're created for when you build
     * a [WorkflowUpdate].
     *
     * @param name allows multiple workflows of the same type to be managed at once. If
     * no name is specified, we unique only on the [Type] itself.
     */
    @PublishedApi
    internal fun makeWorkflowId(name: String = ""): Id<S, E, O> = Id(name, this)

    override fun hashCode(): Int = hashCode
    override fun equals(other: Any?): Boolean = when {
      other === this -> true
      other is Type<*, *, *> -> types.contentEquals(other.types)
      else -> false
    }
  }

  /**
   * When [registered][WorkflowPool.register] with a [WorkflowPool],
   * creates [Workflow] instances as needed.
   *
   * Launchers are registered by and matched to [Id]s by the
   * [tuple of all their type parameters][Type]. This means that only one launcher can be registered
   * for a given combination of [S], [E], and [O] (subsequent launchers will replace
   * previously-registered ones).
   *
   * Instances may be created on demand as a side effect of calls to
   * [WorkflowPool.workflowUpdate]. They are reaped as they are
   * [completed][Workflow.invokeOnCompletion], including via calls to
   * [WorkflowPool.abandonWorkflow].
   */
  @Deprecated("Use com.squareup.workflow.Workflow")
  interface Launcher<S : Any, in E : Any, out O : Any> {
    fun launch(
      initialState: S,
      workflows: WorkflowPool
    ): Workflow<S, E, O>
  }

  /**
   * Unique identifier for a particular [Workflow] to be run by a [WorkflowPool].
   * See [Type.makeWorkflowId] for details.
   */
  data class Id<S : Any, in E : Any, out O : Any>
  internal constructor(
    val name: String,
    val workflowType: Type<S, E, O>
  )

  /**
   * Describes a [Workflow] to be run by a [WorkflowPool]. A [Handle] is effectively
   * a contract, a declaration of the type of [Workflow] that should be run, and what its
   * current state is understood to be.
   *
   * Passing a [Handle] to [awaitWorkflowUpdate] instructs the receiving [WorkflowPool] to
   * make it so: start the described [Workflow] if necessary, and report when its
   * state changes from the given, or when it produces a result.
   *
   * @param id Uniquely identifies this workflow across the [WorkflowPool].
   * @param state The current state of the workflow.
   *
   * @see [handle] to create instances
   */
  data class Handle<S : Any, in E : Any, out O : Any>
  @PublishedApi internal constructor(
    internal val id: Id<S, E, O>,
    val state: S
  )

  private class WorkflowEntry(val workflow: Workflow<*, *, *>)

  private val launchers = mutableMapOf<Type<*, *, *>, Launcher<*, *, *>>()
  private val workflows = mutableMapOf<Id<*, *, *>, WorkflowEntry>()

  @get:TestOnly val peekWorkflowsCount get() = workflows.values.size

  /**
   * Registers the [Launcher] to be used to create workflows that match its [Launcher.workflowType],
   * in response to calls to [workflowUpdate]. A previously registered
   * matching [Launcher] will be replaced, with the intention of allowing redundant calls
   * to be safe.
   */
  fun <S : Any, E : Any, O : Any> register(
    launcher: Launcher<S, E, O>,
    type: Type<S, E, O>
  ) {
    launchers[type] = launcher
  }

  /**
   * Starts the [Workflow] described by [handle] if it wasn't already running.
   * Suspends until that workflow's state changes from the given [Handle.state], or it completes.
   * Returns a [WorkflowUpdate] with the [new state][Running] or the [Finished].
   *
   * Callers are responsible for keeping the [Workflow] running until it produces a
   * [Finished], or else [abandoning][abandonWorkflow] it. To keep the [Workflow]
   * running, call [awaitWorkflowUpdate] again with the [Running.handle] returned from the
   * previous call.
   *
   * @throws kotlinx.coroutines.CancellationException If the nested workflow is
   * [abandoned][abandonWorkflow].
   *
   * @see workflowUpdate
   */
  suspend fun <S : Any, E : Any, O : Any> awaitWorkflowUpdate(
    handle: Handle<S, E, O>
  ): WorkflowUpdate<S, E, O> {
    val workflow = requireWorkflow(handle)
    workflow.openSubscriptionToState()
        .consume {
          removeCompletedWorkflowAfter(handle.id) {
            var state = receiveOrNull()
            // Skip all the states that match the handle's state.
            while (state == handle.state && workflow.isActive) {
              state = receiveOrNull()
            }
            return if (state != null && workflow.isActive) {
              Running(handle.copy(state = state))
            } else {
              Finished(workflow.await())
            }
          }
        }
  }

  /**
   * Suspends until the given [worker] produces its result. If it wasn't already running,
   * starts it with the given [input]. The caller must either consume the result, or
   * else call [abandonWorker].
   *
   * @throws kotlinx.coroutines.CancellationException If the worker is
   * [abandoned][abandonWorker].
   *
   * @see workerResult
   */
  suspend inline fun <reified I : Any, reified O : Any> awaitWorkerResult(
    worker: Worker<I, O>,
    input: I,
    name: String = ""
  ): O = awaitWorkerResult(worker, input, name, worker.workflowType)

  /**
   * Hides the actual logic of starting processes from being inline in external code.
   */
  @PublishedApi
  internal suspend fun <I : Any, O : Any> awaitWorkerResult(
    worker: Worker<I, O>,
    input: I,
    name: String,
    type: Type<I, Nothing, O>
  ): O {
    register(worker.asLauncher(), type)
    val handle = Handle(type.makeWorkflowId(name), input)
    val workflow = requireWorkflow(handle)

    removeCompletedWorkflowAfter(handle.id) {
      return workflow.await()
    }
  }

  /**
   * Returns a [WorkflowInput] that will route events to the identified [Workflow],
   * if it is running. That check is made each time an event is sent: if the workflow
   * is running at the moment, the event is delivered. If not, it is dropped.
   *
   * This method _does not_ start a new workflow if none was running already.
   */
  fun <E : Any> input(
    handle: Handle<*, E, *>
  ): WorkflowInput<E> = object : WorkflowInput<E> {
    override fun sendEvent(event: E) {
      workflows[handle.id]?.let {
        @Suppress("UNCHECKED_CAST")
        (it.workflow as WorkflowInput<E>).sendEvent(event)
      }
    }
  }

  @PublishedApi
  internal fun abandonWorkflow(id: Id<*, *, *>) {
    workflows[id]?.workflow?.cancel()
  }

  /**
   * Abandons the identified workflow if it was already running. If it wasn't, this is a no-op.
   *
   * To abandon a [Worker], use [abandonWorker].
   *
   * @see abandonAll
   */
  fun abandonWorkflow(handle: Handle<*, *, *>) = abandonWorkflow(handle.id)

  /**
   * Abandons the identified [Worker] if it was already running. If it wasn't, this is a no-op.
   *
   * To abandon a nested worker, use [abandonWorker].
   *
   * @see abandonAll
   */
  inline fun <reified I : Any, reified O : Any> abandonWorker(
    worker: Worker<I, O>,
    name: String = ""
  ) = abandonWorkflow(worker.workflowType.makeWorkflowId(name))

  /**
   * Abandons all workflows currently running in this pool.
   *
   * @see abandonWorkflow
   * @see abandonWorker
   */
  fun abandonAll() {
    workflows.values.forEach { it.workflow.cancel() }
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
      }
    }
  }

  companion object {
    /**
     * Creates a [Handle] describing a [Workflow] to be managed via [awaitWorkflowUpdate].
     */
    inline fun <reified S : Any, reified E : Any, reified O : Any> handle(
      launcherType: KClass<out Launcher<S, E, O>>,
      state: S,
      name: String = ""
    ): Handle<S, E, O> = Handle(launcherType.workflowType.makeWorkflowId(name), state)

    /**
     * Creates a [Handle] describing a [Workflow] to be managed via [awaitWorkflowUpdate].
     */
    inline fun <reified E : Any, reified O : Any> handle(
      launcherType: KClass<out Launcher<Unit, E, O>>,
      name: String = ""
    ): Handle<Unit, E, O> = handle(launcherType, Unit, name)
  }
}

/**
 * Registers the [Launcher] to be used to create workflows that match its [Launcher.workflowType],
 * in response to calls to [workflowUpdate]. A previously registered
 * matching [Launcher] will be replaced, with the intention of allowing redundant calls
 * to be safe.
 */
// Note: This is defined as an extension function so that custom register functions can be defined
// that can implement custom behavior for specific launcher sub-types.
inline fun <reified S : Any, reified E : Any, reified O : Any> WorkflowPool.register(
  launcher: Launcher<S, E, O>
) = register(launcher, launcher.workflowType)

/**
 * This is a convenience method that wraps
 * [WorkflowPool.awaitWorkflowUpdate] in a [Deferred] so it can
 * be selected on.
 *
 * @see WorkflowPool.awaitWorkflowUpdate
 */
fun <S : Any, E : Any, O : Any> WorkflowPool.workflowUpdate(
  handle: Handle<S, E, O>
): Deferred<WorkflowUpdate<S, E, O>> = GlobalScope.async(Unconfined) {
  awaitWorkflowUpdate(handle)
}

/**
 * This is a convenience method that wraps [awaitWorkerResult][WorkflowPool.awaitWorkerResult]
 * in a [Deferred] so it can be selected on.
 *
 * @see WorkflowPool.awaitWorkerResult
 */
inline fun <reified I : Any, reified O : Any> WorkflowPool.workerResult(
  worker: Worker<I, O>,
  input: I,
  name: String = ""
): Deferred<O> = workerResult(worker, input, name, worker.workflowType)

/**
 * Hides the implementation of [workerResult] above from being inlined in public code.
 */
@PublishedApi
internal fun <I : Any, O : Any> WorkflowPool.workerResult(
  worker: Worker<I, O>,
  input: I,
  name: String = "",
  type: Type<I, Nothing, O>
) = GlobalScope.async(Unconfined) { awaitWorkerResult(worker, input, name, type) }

/**
 * Returns the [Type] that represents this [Launcher]'s type parameters.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
@Suppress("unused")
inline val <reified S : Any, reified E : Any, reified O : Any>
    Launcher<S, E, O>.workflowType: Type<S, E, O>
  get() = Type(S::class, E::class, O::class)

/**
 * Returns the [Type] that represents this [Launcher] class's type parameters.
 *
 * E.g. `MyLauncher::class.workflowType`
 */
@Suppress("unused")
inline val <reified S : Any, reified E : Any, reified O : Any>
    KClass<out Launcher<S, E, O>>.workflowType: Type<S, E, O>
  get() = Type(S::class, E::class, O::class)

private fun <I : Any, O : Any> Worker<I, O>.asLauncher() = object : Launcher<I, Nothing, O> {
  override fun launch(
    initialState: I,
    workflows: WorkflowPool
  ): Workflow<@UnsafeVariance I, Nothing, O> = GlobalScope.workflow(Unconfined) { _, _ ->
    call(initialState)
  }
}
