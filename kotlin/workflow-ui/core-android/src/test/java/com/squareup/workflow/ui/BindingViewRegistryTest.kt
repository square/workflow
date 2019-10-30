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
    val factory1 = TestViewFactory(FooRendering::class)
    val factory2 = TestViewFactory(BarRendering::class)
    val registry = BindingViewRegistry(factory1, factory2)

    assertThat(registry.keys).containsExactly(factory1.type, factory2.type)
  }

  @Test fun `constructor throws on duplicates`() {
    val factory1 = TestViewFactory(FooRendering::class)
    val factory2 = TestViewFactory(FooRendering::class)

    val error = assertFailsWith<IllegalStateException> {
      BindingViewRegistry(factory1, factory2)
    }
    assertThat(error).hasMessageThat()
        .endsWith("must not have duplicate entries.")
    assertThat(error).hasMessageThat()
        .contains(FooRendering::class.java.name)
  }

  @Test fun `getFactoryFor works`() {
    val fooFactory = TestViewFactory(FooRendering::class)
    val registry = BindingViewRegistry(fooFactory)

    val factory = registry.getFactoryFor(FooRendering::class)
    assertThat(factory).isSameInstanceAs(fooFactory)
  }

  @Test fun `getFactoryFor throws on missing binding`() {
    val fooFactory = TestViewFactory(FooRendering::class)
    val registry = BindingViewRegistry(fooFactory)

    val error = assertFailsWith<IllegalArgumentException> {
      registry.getFactoryFor(BarRendering::class)
    }
    assertThat(error).hasMessageThat()
        .isEqualTo(
            "A ${ViewFactory::class.java.name} should have been registered to display a ${BarRendering::class}."
        )
  }

  @Test fun `ViewRegistry with no arguments infers type`() {
    val registry = ViewRegistry()
    assertTrue(registry.keys.isEmpty())
  }

  private object FooRendering
  private object BarRendering
}
