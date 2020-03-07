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
package com.squareup.sample.recyclerview

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.squareup.sample.recyclerview.AppWorkflow.ChooseRowTypeScreen
import com.squareup.sample.recyclerview.databinding.NewRowTypeItemBinding
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.bindShowRendering
import kotlin.reflect.KClass

/**
 * Shows a list of buttons for selecting a new row type to add.
 */
object ChooseRowTypeViewFactory : ViewFactory<ChooseRowTypeScreen> {
  override val type: KClass<ChooseRowTypeScreen> get() = ChooseRowTypeScreen::class

  override fun buildView(
    initialRendering: ChooseRowTypeScreen,
    initialViewEnvironment: ViewEnvironment,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    val list = LinearLayout(contextForNewView)
    list.orientation = LinearLayout.VERTICAL
    list.setBackgroundColor(Color.WHITE)

    val inflater = LayoutInflater.from(contextForNewView)

    list.bindShowRendering(initialRendering, initialViewEnvironment) { rendering, _ ->
      list.removeAllViews()
      rendering.options.forEachIndexed { index, option ->
        val row = NewRowTypeItemBinding.inflate(inflater, list, false).button
        row.text = option
        row.setOnClickListener { rendering.onSelectionTapped(index) }
        list.addView(row, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
      }
    }

    return list
  }
}
