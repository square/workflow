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
 * [ViewBinding]s that are always available.
 */
internal val defaultViewBindings = ViewRegistry(NamedBinding)

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
 */
interface ViewRegistry {

  /**
   * The set of unique keys which this registry can derive from the renderings passed to [buildView]
   * and for which it knows how to create views.
   *
   * Used to ensure that duplicate bindings are never registered.
   */
  val keys: Set<Any>

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
  ): View

  /**
   * TODO kdoc
   */
  fun <RenderingT : Any> getBindingFor(
    renderingType: KClass<out RenderingT>
  ): ViewBinding<RenderingT>

  companion object : ContainerHintKey<ViewRegistry>(ViewRegistry::class) {
    override val default: ViewRegistry
      get() = error("There should always be a ViewRegistry hint, this is bug in Workflow.")
  }
}

fun ViewRegistry(vararg bindings: ViewBinding<*>): ViewRegistry = BindingViewRegistry(*bindings)

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
 * @throws IllegalArgumentException if no binding can be find for type [RenderingT]
 *
 * @throws IllegalStateException if the matching [ViewBinding] fails to call
 * [View.bindShowRendering] when constructing the view
 */
fun <RenderingT : Any> ViewRegistry.buildView(
  initialRendering: RenderingT,
  initialContainerHints: ContainerHints,
  container: ViewGroup
): View = buildView(initialRendering, initialContainerHints, container.context, container)

operator fun ViewRegistry.plus(binding: ViewBinding<*>): ViewRegistry = this + ViewRegistry(binding)

operator fun ViewRegistry.plus(other: ViewRegistry): ViewRegistry = ViewRegistry(this, other)
