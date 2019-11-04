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
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.sample.recyclerview.R
import com.squareup.sample.recyclerview.inputrows.DropdownInputRow.DropdownHolder
import com.squareup.sample.recyclerview.inputrows.DropdownInputRow.DropdownOption
import com.squareup.sample.recyclerview.inputrows.DropdownInputRow.DropdownOption.Dog

object DropdownInputRow : InputRow<DropdownOption, DropdownHolder> {

  override val description: String get() = "Dropdown Input Row"

  override val defaultValue: DropdownOption get() = Dog

  override fun createViewHolder(parent: ViewGroup): DropdownHolder =
    DropdownHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.dropdown_item, parent, false)
    )

  override fun writeValue(
    holder: DropdownHolder,
    value: DropdownOption
  ) {
    holder.spinner.setSelection(value.ordinal)
  }

  override fun setListener(
    holder: DropdownHolder,
    renderedValue: DropdownOption,
    listener: (DropdownOption) -> Unit
  ) {
    holder.spinner.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>): Unit = error("not supported")

      override fun onItemSelected(
        parent: AdapterView<*>,
        view: View?,
        position: Int,
        id: Long
      ) {
        val selectedOption = DropdownOption.values()[position]
        listener(selectedOption)
      }
    }
  }

  enum class DropdownOption {
    Dog,
    Cat,
    Tree,
    Firehydrant
  }

  class DropdownHolder(view: View) : ViewHolder(view) {
    val spinner: Spinner = view.findViewById(R.id.spinner)

    init {
      spinner.adapter = object : ArrayAdapter<DropdownOption>(
          spinner.context,
          R.layout.support_simple_spinner_dropdown_item,
          android.R.id.text1,
          DropdownOption.values()
      ) {
      }
    }
  }
}
