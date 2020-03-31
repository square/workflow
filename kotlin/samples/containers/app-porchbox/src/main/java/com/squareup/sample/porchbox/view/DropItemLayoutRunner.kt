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
package com.squareup.sample.porchbox.view

import android.view.View
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.squareup.sample.porchbox.DropItemRendering
import com.squareup.sample.porchbox.R
import com.squareup.sample.porchbox.R.layout
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory

/**
 * Renders the screen for entering information about the [Item] dropped, including handles to
 * the input callbacks.
 */
class DropItemLayoutRunner(private val view: View) : LayoutRunner<DropItemRendering> {

  private val emailInput = view.findViewById<TextInputEditText>(R.id.email_input)
  private val nameInput = view.findViewById<TextInputEditText>(R.id.name_input)
  private val uriInput = view.findViewById<TextInputEditText>(R.id.uri_input)
  private val dropButton = view.findViewById<Button>(R.id.drop_button)

  override fun showRendering(
    rendering: DropItemRendering,
    viewEnvironment: ViewEnvironment
  ) {
    dropButton.setOnClickListener {
      rendering.onDropItem(
          emailInput.text?.toString() ?: "",
          nameInput.text?.toString() ?: "empty",
          uriInput.text?.toString() ?: ""
      )
    }
  }

  companion object : ViewFactory<DropItemRendering> by LayoutRunner.bind(
      layout.item_drop_layout,
      ::DropItemLayoutRunner
  )
}