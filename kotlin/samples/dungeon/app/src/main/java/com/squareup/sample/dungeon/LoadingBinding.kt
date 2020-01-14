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
package com.squareup.sample.dungeon

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

@Suppress("FunctionName")
inline fun <reified RenderingT : Any> LoadingBinding(
  @StringRes loadingLabelRes: Int
): ViewBinding<RenderingT> =
  bind(R.layout.loading_layout) { view -> LoadingLayoutRunner<RenderingT>(loadingLabelRes, view) }

@PublishedApi
internal class LoadingLayoutRunner<RenderingT : Any>(
  @StringRes private val labelRes: Int,
  view: View
) : LayoutRunner<RenderingT> {

  private val label: TextView = view.findViewById(R.id.loading_label)

  init {
    label.setText(labelRes)
  }

  override fun showRendering(
    rendering: RenderingT,
    containerHints: ContainerHints
  ) {
    // No-op.
  }
}
