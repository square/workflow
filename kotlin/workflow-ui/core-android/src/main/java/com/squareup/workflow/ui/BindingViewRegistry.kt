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

import kotlin.reflect.KClass

/**
 * A [ViewRegistry] that contains a set of [ViewFactory]s, keyed by the [KClass]es of the
 * rendering types.
 */
internal class BindingViewRegistry private constructor(
  private val bindings: Map<KClass<*>, ViewFactory<*>>
) : ViewRegistry {

  constructor(vararg bindings: ViewFactory<*>) : this(
      bindings.map { it.type to it }
          .toMap()
          .apply {
            check(keys.size == bindings.size) {
              "${bindings.map { it.type }} must not have duplicate entries."
            }
          } as Map<KClass<*>, ViewFactory<*>>
  )

  override val keys: Set<KClass<*>> get() = bindings.keys

  override fun <RenderingT : Any> getFactoryFor(
    renderingType: KClass<out RenderingT>
  ): ViewFactory<RenderingT> {
    @Suppress("UNCHECKED_CAST")
    return requireNotNull(bindings[renderingType] as? ViewFactory<RenderingT>) {
      "A ${ViewFactory::class.java.name} should have been registered " +
          "to display a $renderingType."
    }
  }
}
