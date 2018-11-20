package com.squareup.workflow

import com.squareup.reactor.EnterState
import com.squareup.reactor.FinishWith
import com.squareup.reactor.Reaction
import com.squareup.reactor.WorkflowInput
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.rx2.Workflow
import com.squareup.workflow.rx2.toCompletable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

/**
 * Runs [named][Id] [Workflow] instances, typically those associated with [Delegating]
 * states of composite workflows.
 *
 * @see com.squareup.workflow.rx2.ComposedReactor
 */
class WorkflowPool {
  interface Type<S : Any, E : Any, out O : Any> {
    /**
     * Creates an id for an instance of this type to be managed by a [WorkflowPool].
     *
     * @param name allows multiple workflows of the same type to be managed at once. If
     * no name is specified, we unique only on the [Type] itself.
     */
    fun makeId(name: String = ""): Id<S, E, O> = Id(name, this)
  }

  /**
   * When [registered][WorkflowPool.register] with a [WorkflowPool],
   * creates [Workflow] instances of the specified [type] as needed.
   *
   * Instances may be created on demand as a side effect of calls to
   * [WorkflowPool.nextDelegateReaction]. They are reaped as they
   * are [completed][Workflow.toCompletable], including via calls
   * to [WorkflowPool.abandonDelegate].
   */
  interface Launcher<S : Any, E : Any, out O : Any> {
    val type: Type<S, E, O>

    fun launch(
      initialState: S,
      workflows: WorkflowPool
    ): Workflow<S, E, O>
  }

  /**
   * Unique identifier for a particular [Workflow] to be run by a [WorkflowPool].
   * See [Type.makeId] for details.
   */
  data class Id<S : Any, E : Any, out O : Any>
  internal constructor(
    val name: String,
    val type: Type<S, E, O>
  )

  private class WorkflowEntry(val workflow: Workflow<*, *, *>) {
    val sub = CompositeDisposable()
  }

  private val factories = mutableMapOf<Type<*, *, *>, Launcher<*, *, *>>()
  private val workflows = mutableMapOf<Id<*, *, *>, WorkflowEntry>()

  internal val peekWorkflowsCount get() = workflows.values.size

  /**
   * Registers the [Launcher] to be used to create workflows that match its [Launcher.type],
   * in response to calls to [nextDelegateReaction]. A previously registered
   * matching [Launcher] will be replaced, with the intention of allowing redundant calls
   * to be safe.
   */
  fun <S : Any, E : Any, O : Any> register(factory: Launcher<S, E, O>) {
    factories[factory.type] = factory
  }

  /**
   * Starts the required nested workflow if it wasn't already running. Returns
   * a [Single] that will fire the next time the nested workflow updates its state,
   * or completes.
   *
   * If the nested workflow was not already running, it is started in the
   * [given state][Delegating.delegateState], and that state is reported by the
   * returned [Single]. Otherwise, the [Single] skips state updates that match the given
   * state.
   *
   * If the nested workflow is [abandoned][abandonDelegate], the [Single] never completes.
   */
  fun <S : Any, O : Any> nextDelegateReaction(
    delegating: Delegating<S, *, O>
  ): Single<Reaction<S, O>> {
    val workflow = requireWorkflow(delegating)

    @Suppress("UnstableApiUsage")
    return Maybe
        .amb(listOf(
            workflow.state
                .skipWhile { it == delegating.delegateState }
                .map { EnterState(it) }
                .concatWith(Observable.never())
                .firstElement(),
            workflow.result.map { FinishWith(it) }
        ))
        .switchIfEmpty(Single.never())
  }

  /**
   * Returns a [WorkflowInput] that will route events to the identified [Workflow],
   * if it is running. That check is made each time an event is sent: if the workflow
   * is running at the moment, the event is delivered. If not, it is dropped.
   *
   * This method _does not_ start a new workflow if none was running already.
   */
  fun <E : Any> input(id: Id<*, E, *>): WorkflowInput<E> {
    return object : WorkflowInput<E> {
      override fun sendEvent(event: E) {
        workflows[id]?.let {
          @Suppress("UNCHECKED_CAST")
          (it.workflow as WorkflowInput<E>).sendEvent(event)
        }
      }
    }
  }

  /**
   * Abandons the identified workflow if it was already running. If it wasn't, this
   * is a no-op.
   */
  fun abandonDelegate(id: Id<*, *, *>) {
    workflows[id]?.workflow?.abandon()
  }

  /**
   * Abandons all workflows currently running in this pool.
   */
  fun abandonAll() {
    workflows.values.forEach { it.workflow.abandon() }
  }

  private fun <S : Any, E : Any, O : Any> factory(
    type: Type<S, E, O>
  ): Launcher<S, E, O> {
    val launcher = factories[type]
    check(launcher != null) {
      "Expected launcher for \"$type\". Did you forget to call WorkflowPool.register()?"
    }
    @Suppress("UNCHECKED_CAST")
    return launcher as Launcher<S, E, O>
  }

  private fun <S : Any, E : Any, O : Any> requireWorkflow(
    delegating: Delegating<S, E, O>
  ): Workflow<S, E, O> {
    // Some complexity here to handle workflows that complete the moment
    // they are started. We want to return the short-lived workflow so that its
    // result can be processed, but we also need to make sure it doesn't linger
    // in the map.

    @Suppress("UNCHECKED_CAST")
    var workflow = workflows[delegating.id]?.workflow as Workflow<S, E, O>?

    if (workflow == null) {
      workflow = factory(delegating.id.type).launch(delegating.delegateState, this)

      val entry = WorkflowEntry(workflow)
      workflows[delegating.id] = entry

      entry.sub.add(workflow.toCompletable()
          .subscribe { workflows.remove(delegating.id)!!.sub.dispose() }
      )
    }
    return workflow
  }
}
