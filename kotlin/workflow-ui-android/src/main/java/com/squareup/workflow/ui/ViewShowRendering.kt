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

import android.view.View
import kotlin.reflect.KClass

/**
 * Function attached to a view created by [ViewRegistry], to allow it
 * to respond to [View.showRendering].
 */
typealias ViewShowRendering<RenderingT> = (RenderingT) -> Unit

@ExperimentalWorkflowUi
internal data class ShowRenderingTag<RenderingT : Any>(
  val showRendering: ViewShowRendering<RenderingT>,
  val type: KClass<RenderingT>
)

@ExperimentalWorkflowUi
fun <RenderingT : Any> View.bindShowRendering(
  initialRendering: RenderingT,
  showRendering: ViewShowRendering<RenderingT>
) {
  setTag(
      R.id.view_show_rendering_function,
      ShowRenderingTag(showRendering, initialRendering::class)
  )
  showRendering.invoke(initialRendering)
}

@ExperimentalWorkflowUi
fun View.canShowRendering(rendering: Any): Boolean {
  return showRenderingTag?.type?.isInstance(rendering) == true
}

@ExperimentalWorkflowUi
fun <RenderingT : Any> View.showRendering(rendering: RenderingT) {
  showRenderingTag
      ?.apply {
        check(type.isInstance(rendering)) {
          "Expected instance of ${type.qualifiedName} got $rendering"
        }

        @Suppress("UNCHECKED_CAST")
        (showRendering as ViewShowRendering<RenderingT>).invoke(rendering)
      }
      ?: throw IllegalStateException("Updater for rendering $rendering not found on $this.")
}

@ExperimentalWorkflowUi
private val View.showRenderingTag: ShowRenderingTag<*>?
  get() = getTag(R.id.view_show_rendering_function) as? ShowRenderingTag<*>
