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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

class CompositeViewRegistryTest {

  @Test fun `throws on duplicates`() {
    val fooBarRegistry = TestRegistry(
        mapOf(
            FooRendering::class to mock(),
            BarRendering::class to mock()
        )
    )
    val barBazRegistry = TestRegistry(
        mapOf(
            BarRendering::class to mock(),
            BazRendering::class to mock()
        )
    )

    val error = assertFailsWith<IllegalStateException> {
      CompositeViewRegistry(fooBarRegistry, barBazRegistry)
    }
    assertThat(error).hasMessageThat()
        .startsWith("Must not have duplicate entries: ")
    assertThat(error).hasMessageThat()
        .contains(BarRendering::class.java.name)
  }

  @Test fun `buildView delegates to composite registries`() {
    val fooView = mock<View>()
    val barView = mock<View>()
    val bazView = mock<View>()
    val fooBarRegistry = TestRegistry(
        mapOf(
            FooRendering::class to fooView,
            BarRendering::class to barView
        )
    )
    val bazRegistry = TestRegistry(mapOf(BazRendering::class to bazView))
    val registry = CompositeViewRegistry(fooBarRegistry, bazRegistry)

    assertThat(registry.buildView(FooRendering)).isSameInstanceAs(fooView)
    assertThat(registry.buildView(BarRendering)).isSameInstanceAs(barView)
    assertThat(registry.buildView(BazRendering)).isSameInstanceAs(bazView)
  }

  @Test fun `keys includes all composite registries' keys`() {
    val fooBarRegistry = TestRegistry(
        mapOf(
            FooRendering::class to mock(),
            BarRendering::class to mock()
        )
    )
    val bazRegistry = TestRegistry(mapOf(BazRendering::class to mock()))
    val registry = CompositeViewRegistry(fooBarRegistry, bazRegistry)

    assertThat(registry.keys).containsExactly(
        FooRendering::class,
        BarRendering::class,
        BazRendering::class
    )
  }

  @Test fun `throws on missing binding`() {
    val fooRegistry = TestRegistry(mapOf(FooRendering::class to mock()))
    val registry = CompositeViewRegistry(fooRegistry)

    val error = assertFailsWith<IllegalArgumentException> {
      registry.buildView(BarRendering)
    }
    assertThat(error).hasMessageThat()
        .isEqualTo(
            "A ${ViewBinding::class.java.name} should have been registered to display a ${BarRendering::class}."
        )
  }

  private object FooRendering
  private object BarRendering
  private object BazRendering

  private class TestRegistry(private val bindings: Map<KClass<*>, View>) : ViewRegistry {
    override val keys: Set<Any> get() = bindings.keys

    override fun <RenderingT : Any> buildView(
      initialRendering: RenderingT,
      initialContainerHints: ContainerHints,
      contextForNewView: Context,
      container: ViewGroup?
    ): View = bindings.getValue(initialRendering::class)

    override fun <RenderingT : Any> getBindingFor(
      renderingType: KClass<out RenderingT>
    ): ViewBinding<RenderingT> = throw UnsupportedOperationException()
  }
}
