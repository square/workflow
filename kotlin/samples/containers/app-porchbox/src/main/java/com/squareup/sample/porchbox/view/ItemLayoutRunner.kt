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

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.squareup.picasso.Picasso
import com.squareup.sample.container.masterdetail.MasterDetailConfig
import com.squareup.sample.container.masterdetail.MasterDetailConfig.Detail
import com.squareup.sample.porchbox.ItemRendering
import com.squareup.sample.porchbox.R.id
import com.squareup.sample.porchbox.R.layout
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.backPressedHandler
import com.squareup.workflow.ui.backstack.BackStackConfig
import com.squareup.workflow.ui.backstack.BackStackConfig.None

/**
 * Renders the detail view of an [Item].
 */
class ItemLayoutRunner(private val view: View) : LayoutRunner<ItemRendering> {

  private val toolbar = view.findViewById<Toolbar>(
      id.item_toolbar
  )

  private val name = view.findViewById<TextView>(
      id.item_title
  )
  private val imageView = view.findViewById<ImageView>(
      id.item_image
  )

  private val nextButton = view.findViewById<TextView>(
      id.item_next
  )
  private val previousButton = view.findViewById<TextView>(
      id.item_previous
  )

  private var imageUri: Uri? = null

  override fun showRendering(
    rendering: ItemRendering,
    viewEnvironment: ViewEnvironment
  ) {
    name.text = rendering.name

    rendering.onGoBack
        ?.let {
          previousButton.setOnClickListener { it() }
          previousButton.visibility = View.VISIBLE
        } ?: run {
      previousButton.setOnClickListener(null)
      previousButton.visibility = View.GONE
    }

    rendering.onGoForth
        ?.let {
          nextButton.setOnClickListener { it() }
          nextButton.visibility = View.VISIBLE
        } ?: run {
      nextButton.setOnClickListener(null)
      nextButton.visibility = View.GONE
    }

    // Only reload the image if it has changed
    if (imageUri?.equals(rendering.photoUri) != true) {
      imageUri = rendering.photoUri
      imageUri?.let {
        Picasso.get()
            .load(imageUri)
            .into(imageView)
      }
    }

    if (viewEnvironment[MasterDetailConfig] != Detail && viewEnvironment[BackStackConfig] != None) {
      toolbar.setNavigationOnClickListener { rendering.onGoUp.invoke() }
    } else {
      toolbar.navigationIcon = null
    }

    view.backPressedHandler = rendering.onGoBack
        ?: rendering.onGoUp.takeIf { viewEnvironment[MasterDetailConfig] != Detail }
  }

  companion object : ViewFactory<ItemRendering> by LayoutRunner.bind(
      layout.item_layout,
      ::ItemLayoutRunner
  )
}