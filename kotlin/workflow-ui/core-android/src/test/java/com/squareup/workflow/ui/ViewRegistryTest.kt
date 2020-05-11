package com.squareup.workflow.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

class ViewRegistryTest {

  @Test fun `buildView delegates to ViewFactory`() {
    val fooView = mock<View>()
    val barView = mock<View>()
    val registry = TestRegistry(
        mapOf(
            FooRendering::class to fooView,
            BarRendering::class to barView
        )
    )

    assertThat(registry.buildView(FooRendering))
        .isSameInstanceAs(fooView)
    assertThat(registry.buildView(BarRendering))
        .isSameInstanceAs(barView)
  }

  @Test fun `buildView throws when view not bound`() {
    val view = mock<View> {
      on { toString() } doReturn "mock view"
    }
    val registry = TestRegistry(mapOf(FooRendering::class to view), hasViewBeenBound = false)

    val error = assertFailsWith<IllegalStateException> {
      registry.buildView(FooRendering)
    }
    assertThat(error)
        .hasMessageThat()
        .isEqualTo(
            "View.bindShowRendering should have been called for mock view, typically by the com.squareup.workflow.ui.ViewFactory that created it."
        )
  }

  private object FooRendering
  private object BarRendering

  private class TestRegistry(
    private val bindings: Map<KClass<*>, View>,
    private val hasViewBeenBound: Boolean = true
  ) : ViewRegistry {
    override val keys: Set<KClass<*>> get() = bindings.keys

    override fun <RenderingT : Any> getFactoryFor(
      renderingType: KClass<out RenderingT>
    ): ViewFactory<RenderingT> = object : ViewFactory<RenderingT> {
      @Suppress("UNCHECKED_CAST")
      override val type: KClass<in RenderingT>
        get() = renderingType as KClass<RenderingT>

      override fun buildView(
        initialRendering: RenderingT,
        initialViewEnvironment: ViewEnvironment,
        contextForNewView: Context,
        container: ViewGroup?
      ): View = bindings.getValue(initialRendering::class)
    }

    override fun hasViewBeenBound(view: View): Boolean = hasViewBeenBound
  }
}
