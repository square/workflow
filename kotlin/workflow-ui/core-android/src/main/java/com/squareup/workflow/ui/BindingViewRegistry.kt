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
import kotlin.reflect.KClass

/**
 * A [ViewRegistry] that contains a set of [ViewBinding]s, keyed by the [KClass]es of the rendering
 * types.
 */
internal class BindingViewRegistry private constructor(
  private val bindings: Map<KClass<*>, ViewBinding<*>>
) : ViewRegistry {

  constructor(vararg bindings: ViewBinding<*>) : this(
      bindings.map { it.type to it }.toMap().apply {
        check(keys.size == bindings.size) {
          "${bindings.map { it.type }} must not have duplicate entries."
        }
      } as Map<KClass<*>, ViewBinding<*>>
  )

  override val keys: Set<Any> get() = bindings.keys

  override fun <RenderingT : Any> buildView(
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    @Suppress("UNCHECKED_CAST")
    return getBindingFor(initialRendering::class)
        .buildView(
            initialRendering,
            initialContainerHints,
            contextForNewView,
            container
        )
        .apply {
          checkNotNull(getRendering<RenderingT>()) {
            "View.bindShowRendering should have been called for $this, typically by the " +
                "${ViewBinding::class.java.name} that created it."
          }
        }
  }

  override fun <RenderingT : Any> getBindingFor(
    renderingType: KClass<out RenderingT>
  ): ViewBinding<RenderingT> {
    @Suppress("UNCHECKED_CAST")
    return bindings[renderingType] as? ViewBinding<RenderingT>
        ?: throw IllegalArgumentException(
            "A ${ViewBinding::class.java.name} should have been registered " +
                "to display a $renderingType."
        )
  }
}
