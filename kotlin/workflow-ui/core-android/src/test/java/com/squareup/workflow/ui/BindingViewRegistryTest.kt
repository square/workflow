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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BindingViewRegistryTest {

  @Test fun `keys from bindings`() {
    val binding1 = TestBinding(FooRendering::class)
    val binding2 = TestBinding(BarRendering::class)
    val registry = BindingViewRegistry(binding1, binding2)

    assertThat(registry.keys).containsExactly(binding1.type, binding2.type)
  }

  @Test fun `throws on duplicates`() {
    val binding1 = TestBinding(FooRendering::class)
    val binding2 = TestBinding(FooRendering::class)

    val error = assertFailsWith<IllegalStateException> {
      BindingViewRegistry(binding1, binding2)
    }
    assertThat(error).hasMessageThat()
        .endsWith("must not have duplicate entries.")
    assertThat(error).hasMessageThat()
        .contains(FooRendering::class.java.name)
  }

  @Test fun `throws on missing binding`() {
    val fooBinding = TestBinding(FooRendering::class)
    val registry = BindingViewRegistry(fooBinding)

    val error = assertFailsWith<IllegalArgumentException> {
      registry.buildView(BarRendering)
    }
    assertThat(error).hasMessageThat()
        .isEqualTo(
            "A ${ViewBinding::class.java.name} should have been registered to display a ${BarRendering::class}."
        )
  }

  @Test fun `ViewRegistry with no arguments infers type`() {
    val registry = ViewRegistry()
    assertTrue(registry.keys.isEmpty())
  }

  private object FooRendering
  private object BarRendering
}
