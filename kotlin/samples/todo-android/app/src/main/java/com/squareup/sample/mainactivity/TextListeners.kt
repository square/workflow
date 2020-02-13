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
package com.squareup.sample.mainactivity

import android.content.Context.INPUT_METHOD_SERVICE
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.squareup.sample.todo.R

/**
 * Turns [TextView.addTextChangedListener] into a regular idempotent event handler.
 */
fun TextView.setTextChangedListener(listener: ((String) -> Unit)) {
  getTag(R.id.text_changed_listener)?.let { oldListener ->
    removeTextChangedListener(oldListener as TextWatcher)
  }

  val newListener = object : TextWatcher {
    private lateinit var oldText: String
    override fun afterTextChanged(s: Editable?) {
      if (s.toString() != oldText) listener(s.toString())
    }

    override fun beforeTextChanged(
      s: CharSequence?,
      start: Int,
      count: Int,
      after: Int
    ) {
      oldText = s.toString()
    }

    override fun onTextChanged(
      s: CharSequence?,
      start: Int,
      before: Int,
      count: Int
    ) {
    }
  }
  addTextChangedListener(newListener)
  setTag(R.id.text_changed_listener, newListener)
}

fun View.showSoftKeyboard() {
  val inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
  inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}
