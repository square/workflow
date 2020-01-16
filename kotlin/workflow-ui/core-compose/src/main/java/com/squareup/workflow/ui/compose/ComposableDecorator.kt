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
import com.squareup.workflow.ui.ContainerHintKey

/**
 * Defines a `@Composable` function that will be used to wrap every
 * [composable view binding][bindCompose].
 *
 * This function can install ambients and configure things like theming. It will only be invoked
 * at the top of a given compose tree:
 *  - If multiple nested composable view bindings are used, it will only wrap the top-most one.
 *  - If a composable tree nests a non-composable view binding, which then further nests a
 *    composable binding, the decorator will be invoked twice.
 *
 * The [decorate] function will have access to the [ContainerHintsAmbient].
 */
interface ComposableDecorator {

  @Composable fun decorate(target: @Composable() () -> Unit)

  companion object : ContainerHintKey<ComposableDecorator>(ComposableDecorator::class) {
    override val default: ComposableDecorator = ComposableDecorator { it() }
  }
}

// This could be inline, but that makes the Compose compiler puke.
@Suppress("FunctionName")
fun ComposableDecorator(
  decorator: @Composable() (target: @Composable() () -> Unit) -> Unit
): ComposableDecorator = object : ComposableDecorator {
  @Composable() override fun decorate(target: @Composable() () -> Unit) = decorator(target)
}
