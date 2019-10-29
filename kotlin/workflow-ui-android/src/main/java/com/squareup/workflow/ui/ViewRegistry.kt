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
import com.squareup.workflow.ui.backstack.BackStackContainer
import kotlin.reflect.KClass

/**
 * A collection of [ViewBinding]s that can be used to display the stream of renderings
 * from a workflow tree.
 *
 * Two concrete [ViewBinding] implementations are provided:
 *
 *  - [LayoutRunner.Binding], allowing the easy pairing of Android XML layout resources with
 *    [LayoutRunner]s to drive them.
 *
 *  - [BuilderBinding], which can build views from code.
 *
 *  Registries can be assembled via concatenation, making it easy to snap together screen sets.
 *  For example:
 *
 *     val AuthViewBindings = ViewRegistry(
 *         AuthorizingLayoutRunner, LoginLayoutRunner, SecondFactorLayoutRunner
 *     )
 *
 *     val TicTacToeViewBindings = ViewRegistry(
 *         NewGameLayoutRunner, GamePlayLayoutRunner, GameOverLayoutRunner
 *     )
 *
 *     val ApplicationViewBindings = ViewRegistry(ApplicationLayoutRunner) +
 *         AuthViewBindings + TicTacToeViewBindings
 *
 * In the above example, note that the `companion object`s of the various [LayoutRunner] classes
 * honor a convention of implementing [ViewBinding], in aid of this kind of assembly. See the
 * class doc on [LayoutRunner] for details.
 *
 * Default bindings for the following types are provided, but can be overridden:
 *
 *  - [Named]`<*>` (Delegates to the registered binding for [Named.wrapped].)
 *  - [BackStackScreen]`<*>`
 *  - [AlertContainerScreen]`<*>` (Use [ModalContainer.forAlertContainerScreen] to set
 *    a different dialog theme.)
 */
class ViewRegistry private constructor(
  private val bindings: Map<KClass<*>, ViewBinding<*>>
) {
  /** [bindings] plus any built-ins. Segregated to keep dup checking simple. */
  private val allBindings = bindings +
      (NamedBinding.type to NamedBinding) +
      (BackStackContainer.type to BackStackContainer) +
      (defaultAlertBinding.type to defaultAlertBinding)

  constructor(vararg bindings: ViewBinding<*>) : this(
      bindings.map { it.type to it }.toMap().apply {
        check(keys.size == bindings.size) {
          "${bindings.map { it.type }} must not have duplicate entries."
        }
      }
  )

  constructor(vararg registries: ViewRegistry) : this(
      registries.map { it.bindings }
          .reduce { left, right ->
            val duplicateKeys = left.keys.intersect(right.keys)
            check(duplicateKeys.isEmpty()) { "Must not have duplicate entries: $duplicateKeys." }
            left + right
          }
  )

  /**
   * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
   *
   * Creates a [View] to display [initialRendering], which can be updated via calls
   * to [View.showRendering].
   *
   * @throws IllegalArgumentException if no binding can be find for type [RenderingT]
   *
   * @throws IllegalStateException if the matching [ViewBinding] fails to call
   * [View.bindShowRendering] when constructing the view
   */
  fun <RenderingT : Any> buildView(
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup? = null
  ): View {
    @Suppress("UNCHECKED_CAST")
    return (allBindings[initialRendering::class] as? ViewBinding<RenderingT>)
        ?.buildView(this, initialRendering, initialContainerHints, contextForNewView, container)
        ?.apply {
          checkNotNull(getRendering<RenderingT>()) {
            "View.bindShowRendering should have been called for $this, typically by the " +
                "${ViewBinding::class.java.name} that created it."
          }
        }
        ?: throw IllegalArgumentException(
            "A ${ViewBinding::class.java.name} should have been registered " +
                "to display $initialRendering."
        )
  }

  /**
   * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
   *
   * Creates a [View] to display [initialRendering], which can be updated via calls
   * to [View.showRendering].
   *
   * @throws IllegalArgumentException if no binding can be find for type [RenderingT]
   *
   * @throws IllegalStateException if the matching [ViewBinding] fails to call
   * [View.bindShowRendering] when constructing the view
   */
  fun <RenderingT : Any> buildView(
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    container: ViewGroup
  ): View = buildView(initialRendering, initialContainerHints, container.context, container)

  operator fun <RenderingT : Any> plus(binding: ViewBinding<RenderingT>): ViewRegistry {
    check(binding.type !in bindings.keys) {
      "Already registered ${bindings[binding.type]} for ${binding.type}, cannot accept $binding."
    }
    return ViewRegistry(bindings + (binding.type to binding))
  }

  operator fun plus(registry: ViewRegistry): ViewRegistry {
    return ViewRegistry(this, registry)
  }

  private companion object {
    val defaultAlertBinding = ModalContainer.forAlertContainerScreen()
  }
}

private object NamedBinding : ViewBinding<Named<*>>
by BuilderBinding(
    type = Named::class,
    viewConstructor = { viewRegistry, initialRendering, initialHints, contextForNewView, container ->
      val view =
        viewRegistry.buildView(initialRendering.wrapped, initialHints, contextForNewView, container)
      view.apply {
        @Suppress("RemoveExplicitTypeArguments") // The IDE is wrong.
        val wrappedUpdater = getShowRendering<Any>()!!
        bindShowRendering(initialRendering, initialHints) { r, h ->
          wrappedUpdater.invoke(r.wrapped, h)
        }
      }
    }
)
