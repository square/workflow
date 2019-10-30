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
package com.squareup.workflow.ui.compose

import android.content.Context
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import androidx.compose.Composable
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.WorkflowViewStub
import com.squareup.workflow.ui.compose.ComposableViewStubWrapper.Update

/**
 * Renders [rendering] into the composition using the `ViewRegistry` from the [ContainerHints] to
 * determine how to draw it.
 *
 * To display a nested rendering from a [Composable view binding][bindCompose], use
 * [ContainerHints.showRendering].
 *
 * To preview your layout bindings, use the `ViewBinding.preview` method instead.
 *
 * @see ContainerHints.showRendering
 * @see com.squareup.workflow.ui.ViewRegistry.showRendering
 */
@Composable fun <RenderingT : Any> ViewBinding<RenderingT>.showRendering(
  rendering: RenderingT,
  hints: ContainerHints
) {
  // Fast path: If the child binding is also a Composable, we don't need to go through the legacy
  // view system and can just invoke the binding's composable function directly.
  if (this is ComposeViewBinding) {
    showRendering(rendering, hints)
    return
  }

  // IntelliJ currently complains very loudly about this function call, but it actually compiles.
  // The IDE tooling isn't currently able to recognize that the Compose compiler accepts this code.
  // **Note that this will probably fail, since Compose doesn't currently support rendering legacy
  // views inside any other composable functions (they must be the root composable).
  ComposableViewStubWrapper(update = Update(rendering, hints))
}

/**
 * Wraps a [WorkflowViewStub] with an API that is more Compose-friendly.
 *
 * In particular, Compose will only generate `Emittable`s for views with a single constructor
 * that takes a [Context].
 *
 * See [this slack message](https://kotlinlang.slack.com/archives/CJLTWPH7S/p1576264533012000?thread_ts=1576262311.008800&cid=CJLTWPH7S).
 */
private class ComposableViewStubWrapper(context: Context) : FrameLayout(context) {

  data class Update(
    val rendering: Any,
    val containerHints: ContainerHints
  )

  private val viewStub = WorkflowViewStub(context)

  init {
    addView(viewStub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
  }

  // Compose turns this into a parameter when you invoke this class as a Composable.
  fun setUpdate(update: Update) {
    println("Updating WorkflowViewStub with $update…")
    viewStub.update(update.rendering, update.containerHints)
  }
}
