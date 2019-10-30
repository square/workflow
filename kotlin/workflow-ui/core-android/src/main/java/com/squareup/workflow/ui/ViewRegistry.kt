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
@file:Suppress("FunctionName")

package com.squareup.workflow.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KClass

/**
 * [ViewFactory]s that are always available.
 */
internal val defaultViewFactories = ViewRegistry(NamedBinding)

/**
 * A collection of [ViewFactory]s that can be used to display the stream of renderings
 * from a workflow tree.
 *
 * Two concrete [ViewFactory] implementations are provided:
 *
 *  - [LayoutRunner.Binding], allowing the easy pairing of Android XML layout resources with
 *    [LayoutRunner]s to drive them.
 *
 *  - [BuilderBinding], which can build views from code.
 *
 *  Registries can be assembled via concatenation, making it easy to snap together screen sets.
 *  For example:
 *
 *     val AuthViewFactories = ViewRegistry(
 *         AuthorizingLayoutRunner, LoginLayoutRunner, SecondFactorLayoutRunner
 *     )
 *
 *     val TicTacToeViewFactories = ViewRegistry(
 *         NewGameLayoutRunner, GamePlayLayoutRunner, GameOverLayoutRunner
 *     )
 *
 *     val ApplicationViewFactories = ViewRegistry(ApplicationLayoutRunner) +
 *         AuthViewFactories + TicTacToeViewFactories
 *
 * In the above example, note that the `companion object`s of the various [LayoutRunner] classes
 * honor a convention of implementing [ViewFactory], in aid of this kind of assembly. See the
 * class doc on [LayoutRunner] for details.
 */
interface ViewRegistry {

  /**
   * The set of unique keys which this registry can derive from the renderings passed to [buildView]
   * and for which it knows how to create views.
   *
   * Used to ensure that duplicate bindings are never registered.
   */
  val keys: Set<KClass<*>>

  /**
   * This method is not for general use, use [WorkflowViewStub] instead.
   *
   * Returns the [ViewFactory] that was registered for the given [renderingType].
   *
   * @throws IllegalArgumentException if no factory can be found for type [RenderingT]
   */
  fun <RenderingT : Any> getFactoryFor(
    renderingType: KClass<out RenderingT>
  ): ViewFactory<RenderingT>

  /**
   * This method is not for general use, it's called by [buildView] to validate views returned by
   * [ViewFactory]s.
   *
   * Returns true iff [view] has been bound to a [ShowRenderingTag] by calling [bindShowRendering].
   */
  fun hasViewBeenBound(view: View): Boolean = view.getRendering<Any>() != null

  companion object : ViewEnvironmentKey<ViewRegistry>(ViewRegistry::class) {
    override val default: ViewRegistry
      get() = error("There should always be a ViewRegistry hint, this is bug in Workflow.")
  }
}

fun ViewRegistry(vararg bindings: ViewFactory<*>): ViewRegistry = BindingViewRegistry(*bindings)

/**
 * Returns a [ViewRegistry] that merges all the given [registries].
 */
fun ViewRegistry(vararg registries: ViewRegistry): ViewRegistry = CompositeViewRegistry(*registries)

/**
 * Returns a [ViewRegistry] that contains no bindings.
 *
 * Exists as a separate overload from the other two functions to disambiguate between them.
 */
fun ViewRegistry(): ViewRegistry = BindingViewRegistry()

/**
 * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
 *
 * Creates a [View] to display [initialRendering], which can be updated via calls
 * to [View.showRendering].
 *
 * @throws IllegalArgumentException if no factory can be find for type [RenderingT]
 *
 * @throws IllegalStateException if [ViewRegistry.hasViewBeenBound] returns false (i.e. if the
 * matching [ViewFactory] fails to call [View.bindShowRendering] when constructing the view)
 */
fun <RenderingT : Any> ViewRegistry.buildView(
  initialRendering: RenderingT,
  initialViewEnvironment: ViewEnvironment,
  contextForNewView: Context,
  container: ViewGroup? = null
): View {
  return getFactoryFor(initialRendering::class)
      .buildView(
          initialRendering,
          initialViewEnvironment,
          contextForNewView,
          container
      )
      .apply {
        check(hasViewBeenBound(this)) {
          "View.bindShowRendering should have been called for $this, typically by the " +
              "${ViewFactory::class.java.name} that created it."
        }
      }
}

/**
 * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
 *
 * Creates a [View] to display [initialRendering], which can be updated via calls
 * to [View.showRendering].
 *
 * @throws IllegalArgumentException if no binding can be find for type [RenderingT]
 *
 * @throws IllegalStateException if the matching [ViewFactory] fails to call
 * [View.bindShowRendering] when constructing the view
 */
fun <RenderingT : Any> ViewRegistry.buildView(
  initialRendering: RenderingT,
  initialViewEnvironment: ViewEnvironment,
  container: ViewGroup
): View = buildView(initialRendering, initialViewEnvironment, container.context, container)

operator fun ViewRegistry.plus(binding: ViewFactory<*>): ViewRegistry =
  this + ViewRegistry(binding)

operator fun ViewRegistry.plus(other: ViewRegistry): ViewRegistry = ViewRegistry(this, other)
