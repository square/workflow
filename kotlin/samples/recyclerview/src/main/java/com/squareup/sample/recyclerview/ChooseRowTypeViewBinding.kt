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
import android.widget.Button
import android.widget.LinearLayout
import com.squareup.sample.recyclerview.AppWorkflow.ChooseRowTypeScreen
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.bindShowRendering
import kotlin.reflect.KClass

/**
 * Shows a list of buttons for selecting a new row type to add.
 */
object ChooseRowTypeViewBinding : ViewBinding<ChooseRowTypeScreen> {
  override val type: KClass<ChooseRowTypeScreen> get() = ChooseRowTypeScreen::class

  override fun buildView(
    registry: ViewRegistry,
    initialRendering: ChooseRowTypeScreen,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    val list = LinearLayout(contextForNewView)
    list.orientation = LinearLayout.VERTICAL
    list.setBackgroundColor(Color.WHITE)

    val inflator = LayoutInflater.from(contextForNewView)

    list.bindShowRendering(initialRendering, initialContainerHints) { rendering, _ ->
      list.removeAllViews()
      rendering.options.forEachIndexed { index, option ->
        val row = inflator.inflate(R.layout.new_row_type_item, list, false) as Button
        row.text = option
        row.setOnClickListener { rendering.onSelectionTapped(index) }
        list.addView(row, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
      }
    }

    return list
  }
}
