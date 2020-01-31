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
package com.squareup.workflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// If you try to replace isTrue() with isTrue compilation fails.
@Suppress("UsePropertyAccessSyntax")
class CompatibleTest {
  private object Able
  private object Baker
  private object Charlie

  @Test fun `Different types do not match`() {
    val able = object : Any() {}
    val baker = object : Any() {}

    assertThat(compatible(able, baker)).isFalse()
  }

  @Test fun `Same type matches`() {
    assertThat(compatible("Able", "Baker")).isTrue()
  }

  @Test fun `isCompatibleWith is honored`() {
    data class K(override val compatibilityKey: String) : Compatible

    assertThat(compatible(K("hey"), K("hey"))).isTrue()
    assertThat(compatible(K("hey"), K("ho"))).isFalse()
  }

  @Test fun `Different Compatible types do not match`() {
    abstract class A : Compatible

    class Able(override val compatibilityKey: String) : A()
    class Alpha(override val compatibilityKey: String) : A()

    assertThat(compatible(Able("Hey"), Alpha("Hey"))).isFalse()
  }

  @Test fun `goTo pushes`() {
    val stack = listOf<Any>(Able).goTo(Baker)
        .goTo(Charlie)

    assertThat(stack).isEqualTo(listOf(Able, Baker, Charlie))
  }

  @Test fun `goTo pops to bottom`() {
    val stack = listOf<Any>(Able).goTo(Baker)
        .goTo(Charlie)
        .goTo(Able)

    assertThat(stack).isEqualTo(listOf(Able))
  }

  @Test fun `goTo pops to middle`() {
    val stack = listOf<Any>(Able).goTo(Baker)
        .goTo(Charlie)
        .goTo(Baker)

    assertThat(stack).isEqualTo(listOf(Able, Baker))
  }

  @Test fun `goTo Named works`() {
    val originalAble = Named(Able, "one")
    val stack = listOf<Any>(
        originalAble,
        Named(Able, "two"),
        Named(Able, "three")
    ).goTo(Named(Able, "one"))

    assertThat(stack).hasSize(1)
    assertThat(stack[0]).isSameInstanceAs(originalAble)
  }
}
