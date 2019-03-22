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
package com.squareup.workflow.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KTypesTest {

  @Test fun `fromGenericType equivalence`() {
    val type1 = KTypes.fromGenericType(List::class, String::class)
    val type2 = KTypes.fromGenericType(List::class, String::class)

    assertEquals(type1, type2)
  }

  @Test fun `fromGenericType unequal type`() {
    val type1 = KTypes.fromGenericType(List::class, String::class)
    val type2 = KTypes.fromGenericType(Set::class, String::class)

    assertNotEquals(type1, type2)
  }

  @Test fun `fromGenericType unequal type argument`() {
    val type1 = KTypes.fromGenericType(List::class, String::class)
    val type2 = KTypes.fromGenericType(List::class, Int::class)

    assertNotEquals(type1, type2)
  }
}
