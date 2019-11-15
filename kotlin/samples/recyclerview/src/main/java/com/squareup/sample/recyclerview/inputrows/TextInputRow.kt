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

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.sample.recyclerview.R
import com.squareup.sample.recyclerview.inputrows.TextInputRow.TextHolder

object TextInputRow : InputRow<String, TextHolder> {

  override val description: String get() = "Text Input Row"

  override val defaultValue: String get() = ""

  override fun createViewHolder(parent: ViewGroup): TextHolder =
    TextHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.text_item, parent, false)
    )

  override fun writeValue(
    holder: TextHolder,
    value: String
  ) {
    val currentText = holder.editText.text.toString()
    if (currentText != value) {
      holder.editText.editableText.replace(0, currentText.length, value)
    }
  }

  override fun setListener(
    holder: TextHolder,
    renderedValue: String,
    listener: (String) -> Unit
  ) {
    holder.textChangedListener = {
      listener(it)
    }
  }

  class TextHolder(view: View) : ViewHolder(view) {

    val editText: EditText = view.findViewById(R.id.edittext)
    var textChangedListener: (String) -> Unit = {}

    init {
      editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
          textChangedListener(s.toString())
        }

        override fun beforeTextChanged(
          s: CharSequence?,
          start: Int,
          count: Int,
          after: Int
        ) = Unit

        override fun onTextChanged(
          s: CharSequence?,
          start: Int,
          before: Int,
          count: Int
        ) = Unit
      })
    }
  }
}
