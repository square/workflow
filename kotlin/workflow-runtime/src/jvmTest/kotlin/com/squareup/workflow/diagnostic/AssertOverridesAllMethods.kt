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
package com.squareup.workflow.diagnostic

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Asserts that all methods declared by [superType] are overridden by [type].
 *
 * Uses Kotlin reflection instead of Java reflection, because Java reflection will return all
 * methods with default interface implementations as "declared" on subclasses.
 */
fun <I : Any, T : I> assertOverridesAllMethods(
  type: KClass<T>,
  superType: KClass<I>
) {
  val expectedMethodNames = superType.declaredMemberFunctions.map { it.name }
  val declaredMethodNames = type.declaredMemberFunctions.map { it.name }

  expectedMethodNames.forEach { expectedMethod ->
    assertTrue(
        expectedMethod in declaredMethodNames,
        "Expected $type to implement $expectedMethod"
    )
  }
}

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
