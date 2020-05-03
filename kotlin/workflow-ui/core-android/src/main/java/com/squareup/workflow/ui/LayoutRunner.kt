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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import kotlin.reflect.KClass

typealias ViewBindingInflater<BindingT> = (LayoutInflater, ViewGroup?, Boolean) -> BindingT

/**
 * A delegate that implements a [showRendering] method to be called when a workflow rendering
 * of type [RenderingT] is ready to be displayed in a view inflated from a layout resource
 * by a [ViewRegistry]. (Use [BuilderBinding] if you want to build views from code rather
 * than layouts.)
 *
 * Typical usage is to have a [LayoutRunner]'s `companion object` implement
 * [ViewFactory] by delegating to [LayoutRunner.bind], specifying the layout resource
 * it expects to drive.
 *
 *   class HelloLayoutRunner(view: View) : LayoutRunner<Rendering> {
 *     private val messageView: TextView = view.findViewById(R.id.hello_message)
 *
 *     override fun showRendering(rendering: Rendering) {
 *       messageView.text = rendering.message
 *       messageView.setOnClickListener { rendering.onClick(Unit) }
 *     }
 *
 *     companion object : ViewFactory<Rendering> by bind(
 *         R.layout.hello_goodbye_layout, ::HelloLayoutRunner
 *     )
 *   }
 *
 * This pattern allows us to assemble a [ViewRegistry] out of the [LayoutRunner] classes
 * themselves.
 *
 *    val TicTacToeViewBuilders = ViewRegistry(
 *        NewGameLayoutRunner, GamePlayLayoutRunner, GameOverLayoutRunner
 *    )
 *
 * ## AndroidX ViewBinding
 *
 * [AndroidX ViewBinding][ViewBinding] is supported in two ways.
 * In most cases, you can use the `bind` function that takes a function and avoid implementing
 * [LayoutRunner] at all.
 *
 * If you need to perform some set up before [showRendering] is called, use the
 * `bind` overload that takes:
 *  - a reference to a `ViewBinding.inflate` method and
 *  - a [LayoutRunner] constructor that accepts a [ViewBinding]
 *
 *   class HelloLayoutRunner(private val binding: HelloGoodbyeLayoutBinding) : LayoutRunner<Rendering> {
 *
 *     override fun showRendering(rendering: Rendering) {
 *       binding.messageView.text = rendering.message
 *       binding.messageView.setOnClickListener { rendering.onClick(Unit) }
 *     }
 *
 *     companion object : ViewFactory<Rendering> by bind(
 *         HelloGoodbyeLayoutBinding::inflate, ::HelloLayoutRunner
 *     )
 *   }
 *
 * If the view does not need to be initialized, the [bind] function can be used instead.
 */
interface LayoutRunner<RenderingT : Any> {
  fun showRendering(
    rendering: RenderingT,
    viewEnvironment: ViewEnvironment
  )

  class Binding<RenderingT : Any>(
    override val type: KClass<RenderingT>,
    @LayoutRes private val layoutId: Int,
    private val runnerConstructor: (View) -> LayoutRunner<RenderingT>
  ) : ViewFactory<RenderingT> {
    override fun buildView(
      initialRendering: RenderingT,
      initialViewEnvironment: ViewEnvironment,
      contextForNewView: Context,
      container: ViewGroup?
    ): View {
      return contextForNewView.viewBindingLayoutInflater(container)
          .inflate(layoutId, container, false)
          .apply {
            bindShowRendering(
                initialRendering,
                initialViewEnvironment,
                runnerConstructor.invoke(this)::showRendering
            )
          }
    }
  }

  companion object {
    /**
     * Creates a [ViewFactory] that inflates [layoutId] to show renderings of type [RenderingT],
     * using a [LayoutRunner] created by [constructor].
     */
    inline fun <reified RenderingT : Any> bind(
      @LayoutRes layoutId: Int,
      noinline constructor: (View) -> LayoutRunner<RenderingT>
    ): ViewFactory<RenderingT> = Binding(RenderingT::class, layoutId, constructor)

    /**
     * Creates a [ViewFactory] that [inflates][bindingInflater] a [ViewBinding] ([BindingT]) to show
     * renderings of type [RenderingT], using [showRendering].
     *
     * ```
     * val HelloBinding: ViewFactory<Rendering> =
     *   bindViewBinding(HelloGoodbyeLayoutBinding::inflate) { rendering, containerHints ->
     *     helloMessage.text = rendering.message
     *     helloMessage.setOnClickListener { rendering.onClick(Unit) }
     *   }
     * ```
     *
     * If you need to initialize your view before [showRendering] is called, create a [LayoutRunner]
     * and create a binding using `LayoutRunner.bind` instead.
     */
    inline fun <BindingT : ViewBinding, reified RenderingT : Any> bind(
      noinline bindingInflater: ViewBindingInflater<BindingT>,
      crossinline showRendering: BindingT.(RenderingT, ViewEnvironment) -> Unit
    ): ViewFactory<RenderingT> = bind(bindingInflater) { binding ->
      object : LayoutRunner<RenderingT> {
        override fun showRendering(
          rendering: RenderingT,
          viewEnvironment: ViewEnvironment
        ) = binding.showRendering(rendering, viewEnvironment)
      }
    }

    /**
     * Creates a [ViewFactory] that [inflates][bindingInflater] a [BindingT] to show renderings of
     * type [RenderingT], using a [LayoutRunner] created by [constructor].
     *
     * If the view doesn't need to be initialized before [showRendering] is called,
     * [bind] can be used instead, which just takes a lambda instead requiring a whole
     * [LayoutRunner] class.
     */
    inline fun <BindingT : ViewBinding, reified RenderingT : Any> bind(
      noinline bindingInflater: ViewBindingInflater<BindingT>,
      noinline constructor: (BindingT) -> LayoutRunner<RenderingT>
    ): ViewFactory<RenderingT> =
      ViewBindingViewFactory(RenderingT::class, bindingInflater, constructor)

    /**
     * Creates a [ViewFactory] that inflates [layoutId] to "show" renderings of type [RenderingT],
     * with a no-op [LayoutRunner]. Handy for showing static views.
     */
    inline fun <reified RenderingT : Any> bindNoRunner(
      @LayoutRes layoutId: Int
    ): ViewFactory<RenderingT> = bind(layoutId) {
      object : LayoutRunner<RenderingT> {
        override fun showRendering(
          rendering: RenderingT,
          viewEnvironment: ViewEnvironment
        ) = Unit
      }
    }
  }
}

internal fun Context.viewBindingLayoutInflater(container: ViewGroup?) =
  LayoutInflater.from(container?.context ?: this)
      .cloneInContext(this)
