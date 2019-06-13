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
package com.squareup.sample

import com.squareup.workflow.Workflow

/**
 * A [Workflow] that can be rendered by a [ScreenContext].
 * Navigates by placing the [GoBackHandler] from its [input][ScreenInput] in its rendering to go
 * back, and by returning a non-null [GoBackHandler] in its [rendering][ScreenRendering] to go
 * forward.
 */
typealias ScreenWorkflow<I, O, R> = Workflow<ScreenInput<I>, O, ScreenRendering<R>>

/**
 * The input to a [ScreenWorkflow], and the event handler to invoke to navigate back past this
 * screen.
 *
 * @param goBackHandler If non-null, should be used as the event handler for navigating back past
 * this screen. If null, this is the first screen and back navigation is not allowed.
 */
data class ScreenInput<I>(
  val input: I,
  val goBackHandler: GoBackHandler
)

/**
 * The rendering from a [ScreenWorkflow], along with an optional event handler that, if present,
 * will hint at the parent to render the next workflow, and undo that hint when invoked.
 *
 * @param goBackHandler If non-null, indicates that the rendered [ScreenWorkflow] has completed
 * its screen and the next screen should be shown.
 */
data class ScreenRendering<R>(
  val screenRendering: R,
  val goBackHandler: GoBackHandler?
)

/**
 * A (`Unit`) event handler that goes back to a screen, with a description about what screen it goes
 * back to.
 */
data class GoBackHandler(
  val targetDescription: String,
  val goBack: (Unit) -> Unit
) {
  fun goBack() = goBack(Unit)

  override fun toString(): String = "GoBackHandler(to $targetDescription)"
}
