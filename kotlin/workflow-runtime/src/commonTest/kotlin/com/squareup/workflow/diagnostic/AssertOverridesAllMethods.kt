package com.squareup.workflow.diagnostic

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Asserts that all methods declared by [superType] are overridden by [type].
 *
 * Uses Kotlin reflection instead of Java reflection, because Java reflection will return all
 * methods with default interface implementations as "declared" on subclasses.
 */
expect fun <I : Any, T : I> assertOverridesAllMethods(
  type: KClass<T>,
  superType: KClass<I>
)

class AssertOverridesAllMethodsTest {
  @Test fun `fails on missing method`() {
    val failure = assertFailsWith<AssertionError> {
      assertOverridesAllMethods(Implementation::class, Interface::class)
    }
    assertEquals("Expected ${Implementation::class} to implement method", failure.message)
  }

  private interface Interface {
    fun method() = Unit
  }

  private class Implementation : Interface
}
