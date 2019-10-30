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
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

class CompositeViewRegistryTest {

  @Test fun `constructor throws on duplicates`() {
    val fooBarRegistry = TestRegistry(setOf(FooRendering::class, BarRendering::class))
    val barBazRegistry = TestRegistry(setOf(BarRendering::class, BazRendering::class))

    val error = assertFailsWith<IllegalStateException> {
      CompositeViewRegistry(fooBarRegistry, barBazRegistry)
    }
    assertThat(error).hasMessageThat()
        .startsWith("Must not have duplicate entries: ")
    assertThat(error).hasMessageThat()
        .contains(BarRendering::class.java.name)
  }

  @Test fun `getFactoryFor delegates to composite registries`() {
    val fooFactory = TestViewFactory(FooRendering::class)
    val barFactory = TestViewFactory(BarRendering::class)
    val bazFactory = TestViewFactory(BazRendering::class)
    val fooBarRegistry = TestRegistry(
        mapOf(
            FooRendering::class to fooFactory,
            BarRendering::class to barFactory
        )
    )
    val bazRegistry = TestRegistry(factories = mapOf(BazRendering::class to bazFactory))
    val registry = CompositeViewRegistry(fooBarRegistry, bazRegistry)

    assertThat(registry.getFactoryFor(FooRendering::class))
        .isSameInstanceAs(fooFactory)
    assertThat(registry.getFactoryFor(BarRendering::class))
        .isSameInstanceAs(barFactory)
    assertThat(registry.getFactoryFor(BazRendering::class))
        .isSameInstanceAs(bazFactory)
  }

  @Test fun `getFactoryFor throws on missing registry`() {
    val fooRegistry = TestRegistry(setOf(FooRendering::class))
    val registry = CompositeViewRegistry(fooRegistry)

    val error = assertFailsWith<IllegalArgumentException> {
      registry.getFactoryFor(BarRendering::class)
    }
    assertThat(error).hasMessageThat()
        .isEqualTo(
            "A ${ViewFactory::class.java.name} should have been registered to display a ${BarRendering::class}."
        )
  }

  @Test fun `keys includes all composite registries' keys`() {
    val fooBarRegistry = TestRegistry(setOf(FooRendering::class, BarRendering::class))
    val bazRegistry = TestRegistry(setOf(BazRendering::class))
    val registry = CompositeViewRegistry(fooBarRegistry, bazRegistry)

    assertThat(registry.keys).containsExactly(
        FooRendering::class,
        BarRendering::class,
        BazRendering::class
    )
  }

  private object FooRendering
  private object BarRendering
  private object BazRendering

  private class TestRegistry(private val factories: Map<KClass<*>, ViewFactory<*>>) : ViewRegistry {
    constructor(keys: Set<KClass<*>>) : this(keys.associateWith { TestViewFactory(it) })

    override val keys: Set<KClass<*>> get() = factories.keys

    @Suppress("UNCHECKED_CAST")
    override fun <RenderingT : Any> getFactoryFor(
      renderingType: KClass<out RenderingT>
    ): ViewFactory<RenderingT> = factories.getValue(renderingType) as ViewFactory<RenderingT>
  }
}
