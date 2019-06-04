package com.squareup.workflow.testing

/**
 * Wraps a function that accepts an argument of type [T], but the returned function will be
 * considered equal to all other functions returned from this function.
 *
 * All functions returned from this are considered equal (and have the same hashcode), which means
 * they can be stored in data classes and will not affect the equivalence of the rest of the data
 * class's properties. This makes it much easier to write test assertions for workflow renderings
 * that include event handlers, since you can compare entire rendering types at once and not
 * field-by-field.
 *
 * E.g.
 * ```
 * val foo = indistinctFunction { println("foo($it)") }
 * val bar = indistinctFunction { println("bar($it)") }
 * assertTrue(foo == bar)
 * assertTrue(foo !== bar)
 * ```
 */
fun <T> indistinctFunction(block: (T) -> Unit): (T) -> Unit = IndistinctFunction(block)

private class IndistinctFunction<in T>(private val handler: (T) -> Unit) : (T) -> Unit {

  override fun invoke(event: T) = handler(event)

  /**
   * Returns `true` iff `other` is an [IndistinctFunction] – all [IndistinctFunction]s are
   * considered equal.
   */
  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is IndistinctFunction<*> -> true
    else -> false
  }

  /**
   * All [IndistinctFunction]s have the same hashcode, since they are all equal.
   */
  override fun hashCode(): Int = 0

  override fun toString(): String = "IndistinctFunction@${super.hashCode()}"
}
