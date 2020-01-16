/*
 * Copyright 2020 Square Inc.
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
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.workflow.ui.compose

import androidx.compose.Composable
import com.squareup.workflow.ui.ViewEnvironmentKey

// TODO a better way to do this whole thing is for ComposeViewFactory to look for a composition
// reference in its ancestor somehow, and use that to propogate ambients and stuff down to itself.

/**
 * Defines a `@Composable` function that will be used to wrap every
 * [composable view binding][bindCompose].
 *
 * This function can install ambients and configure things like theming. It will only be invoked
 * at the top of a given compose tree:
 *  - If multiple nested composable view bindings are used, it will only wrap the top-most one.
 *  - If a composable tree nests a non-composable view binding, which then further nests a
 *    composable binding, the decorator will be invoked twice.
 */
interface ComposableDecorator {

  @Composable fun decorate(content: @Composable() () -> Unit)

  companion object : ViewEnvironmentKey<ComposableDecorator>(ComposableDecorator::class) {
    override val default: ComposableDecorator get() = NoopComposableDecorator
  }
}

// This could be inline, but that makes the Compose compiler puke.
@Suppress("FunctionName")
fun ComposableDecorator(
  decorator: @Composable() (content: @Composable() () -> Unit) -> Unit
): ComposableDecorator = object : ComposableDecorator {
  @Composable override fun decorate(content: @Composable() () -> Unit) = decorator(content)
}

/**
 * [ComposableDecorator] that asserts that the [decorate] method invokes its children parameter
 * exactly once, and throws an [IllegalStateException] if not.
 */
internal class AssertingComposableDecorator(
  private val delegate: ComposableDecorator
) : ComposableDecorator {
  @Composable override fun decorate(content: @Composable() () -> Unit) {
    var childrenCalledCount = 0
    delegate.decorate {
      childrenCalledCount++
      content()
    }
    // TODO do we need to `remember` anything here? I think we don't because we want to assert
    // on every compose pass, in case delegate has conditionals.
    check(childrenCalledCount == 1) {
      "Expected ComposableDecorator to invoke children exactly once, " +
          "but was invoked $childrenCalledCount times."
    }
  }
}

private object NoopComposableDecorator : ComposableDecorator {
  @Composable override fun decorate(content: () -> Unit) {
    content()
  }
}
