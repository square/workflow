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

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.sample.recyclerview.databinding.RecyclerviewLayoutBinding
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.Rendering
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment

class EditableListLayoutRunner(binding: RecyclerviewLayoutBinding) : LayoutRunner<Rendering> {
  private val adapter = EditableListAdapter()

  init {
    with(binding.recyclerview) {
      layoutManager = LinearLayoutManager(binding.root.context)
      adapter = this@EditableListLayoutRunner.adapter
      itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }
    }
  }

  override fun showRendering(
    rendering: Rendering,
    viewEnvironment: ViewEnvironment
  ) {
    adapter.updateRendering(rendering, viewEnvironment[ListDiffMode])
  }

  companion object : ViewFactory<Rendering> by bind(
      RecyclerviewLayoutBinding::inflate, ::EditableListLayoutRunner
  )
}
