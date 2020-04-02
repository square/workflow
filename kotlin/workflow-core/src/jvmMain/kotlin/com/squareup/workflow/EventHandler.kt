@file:Suppress("DEPRECATION")

package com.squareup.workflow

/**
 * Wraps a function that handles an event of type [EventT]. Implements the raw function type
 * `(EventT) -> Unit`, so instances can be invoked and passed around as functions.
 *
 * This type differs from a typical function reference in how it defines equality. All instances
 * of this class are considered equal (and have the same hashcode), which means they can be stored
 * in data classes and the event handlers will be ignored. This makes it much easier to write
 * test assertions for [Workflow] renderings that include event handlers, since you can compare
 * entire rendering types at once and not field-by-field.
 */
@Deprecated("obsolete")
class EventHandler<in EventT>(private val handler: (EventT) -> Unit) : (EventT) -> Unit {

  override fun invoke(event: EventT) = handler(event)

  /**
   * Returns `true` iff `other` is an [EventHandler] – all [EventHandler]s are considered equal.
   */
  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is EventHandler<*> -> true
    else -> false
  }

  /**
   * All [EventHandler]s have the same hashcode, since they are all equal.
   */
  override fun hashCode(): Int = 0

  override fun toString(): String = "EventHandler@${super.hashCode()}"
}

/**
 * [EventHandler]s of type `Unit` are effectively no-arg functions, so this override lets you
 * invoke them without passing the `Unit` argument.
 */
operator fun EventHandler<Unit>.invoke() = invoke(Unit)
