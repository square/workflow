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
package com.squareup.sample.recyclerview.editablelistworkflow

import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.sample.recyclerview.R
import com.squareup.sample.recyclerview.R.id
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.Rendering
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

class EditableListLayoutRunner(view: View) : LayoutRunner<Rendering> {
  private val adapter = EditableListAdapter()

  init {
    view.findViewById<RecyclerView>(id.recyclerview)
        .apply {
          layoutManager = LinearLayoutManager(view.context)
          adapter = this@EditableListLayoutRunner.adapter
          itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }
        }
  }

  override fun showRendering(
    rendering: Rendering,
    containerHints: ContainerHints
  ) {
    adapter.updateRendering(rendering, containerHints[ListDiffMode])
  }

  companion object : ViewBinding<Rendering> by bind(
      R.layout.recyclerview_layout, ::EditableListLayoutRunner
  )
}
