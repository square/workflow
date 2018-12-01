package com.squareup.workflow

import com.squareup.workflow.rx2.ofKeyType
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import kotlin.reflect.jvm.jvmName

typealias AnyScreen = Screen<*, *>
typealias AnyScreenKey = Screen.Key<*, *>

/**
 * **NB:** This class is not quite deprecated yet, but it will be _very_ soon.
 *
 * A platform independent view model, typically instantiated via a [Renderer].
 *
 * While not a `data class`, this class still supports destructuring so you can do stuff like:
 * `val (data, workflow) = screen` ([key] is not destructure-able).
 *
 * @param data A complete rendering of this screen.
 * @param workflow The object that accepts events for this screen.
 */
class Screen<D : Any, E : Any>(
  @JvmField val key: Key<D, E>,
  @JvmField val data: D,
  @JvmField val workflow: WorkflowInput<E>
) {

  override fun toString(): String {
    return "${super.toString()}($key)"
  }

  operator fun component1() = data
  operator fun component2() = workflow

  /**
   * Uniquely identifies a type of screen and defines its schema. Also can [filter]
   * an untyped stream of [Screen] down to just those that match.
   *
   * @param typeName Uniquely identifies screens of this type, typically a class name. All
   * keys with the same `typeName` must really be of the same type.
   * @param value An optional value, handy when dynamic keys are needed.
   */
  data class Key<D : Any, E : Any>(
    @JvmField val typeName: String,
    @JvmField val value: String = ""
  ) {

    /**
     * Convenience for the common case of defining a key as a member of an `object`.
     * E.g., look at the KEY declaration in this snippet:
     *
     *    typealias FooBarScreen = WorkflowScreen<FooBar.Data, FooBar.Events>
     *
     *    object FooBar {
     *      val KEY = WorkflowScreen.Key<Data, Events>(this)
     *
     *      // Define the Data and Event types here.
     *
     *      fun createScreen(
     *        data: Data,
     *        eventHandler: Events
     *      ) = FooBarScreen(KEY, data, events)
     *    }
     */
    constructor(wrapper: Any) : this(wrapper::class.jvmName)

    /**
     * Convenience for Java code, allows [ofKeyType] to be used via [Observable.compose].
     *
     *    myWorkflow.getState()
     *        .map(wfState -> wfState.state[SOME_LAYER]!!)
     *        .compose(key.filter())
     */
    fun filter(): ObservableTransformer<AnyScreen, Screen<D, E>> =
      ObservableTransformer { observable -> observable.ofKeyType(this) }

    /**
     * Returns true if [otherKey] has the same [typeName] as this one, in which
     * case it solemnly pinkie-swears that it is also of the same type.
     */
    private fun matchesTypeOf(otherKey: AnyScreenKey) = otherKey.typeName == this.typeName

    /**
     * If the given key has the same [typeName] as this one, cast it to match the type of this one,
     * or else return null.
     */
    fun castOrNull(otherKey: AnyScreenKey): Key<D, E>? {
      // We promise that keys with matching typeNames really are of the same type.
      @Suppress("UNCHECKED_CAST")
      return otherKey.takeIf { matchesTypeOf(it) } as Key<D, E>?
    }

    /**
     * Casts the given key, which must have the same [typeName] as this one, to the
     * declared type of this one.
     *
     * @throws IllegalArgumentException if [otherKey] is of the wrong type.
     */
    fun cast(otherKey: AnyScreenKey): Key<D, E> {
      return castOrNull(otherKey) ?: throw IllegalArgumentException(
          "$otherKey cannot be cast to $this"
      )
    }
  }

  companion object {
    @JvmField
    val NO_KEY = Key<Nothing, Nothing>("NO_KEY")
  }
}
