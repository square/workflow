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
package com.squareup.sample.recyclerview.inputrows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.sample.recyclerview.R
import com.squareup.sample.recyclerview.inputrows.SwitchInputRow.SwitchHolder

object SwitchInputRow : InputRow<Boolean, SwitchHolder> {

  override val description: String get() = "Switch Input Row"

  override val defaultValue: Boolean get() = false

  override fun createViewHolder(parent: ViewGroup): SwitchHolder = SwitchHolder(
      LayoutInflater.from(parent.context)
          .inflate(R.layout.switch_item, parent, false)
  )

  override fun writeValue(
    holder: SwitchHolder,
    value: Boolean
  ) {
    holder.view.isChecked = value
  }

  override fun setListener(
    holder: SwitchHolder,
    renderedValue: Boolean,
    listener: (Boolean) -> Unit
  ) {
    holder.view.setOnCheckedChangeListener { _, isChecked ->
      listener(isChecked)
    }
  }

  class SwitchHolder(view: View) : ViewHolder(view) {
    val view: SwitchCompat = view.findViewById(R.id.switchview)
  }
}
