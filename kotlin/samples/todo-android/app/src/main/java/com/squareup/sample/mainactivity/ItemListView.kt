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

import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import com.squareup.sample.todo.R

class ItemListView private constructor(private val itemContainer: LinearLayout) {

  private val inflater = LayoutInflater.from(itemContainer.context)
  private val strikethroughSpan = StrikethroughSpan()

  var onDoneClickedListener: (index: Int) -> Unit = {}
  var onTextChangedListener: (index: Int, text: String) -> Unit = { _, _ -> }
  var onDeleteClickedListener: (index: Int) -> Unit = {}

  fun setRows(rows: List<Pair<Boolean, String>>) {
    for ((index, row) in rows.withIndex()) {
      addItemRow(index, row.first, row.second)
    }
    pruneDeadRowsFrom(rows.size)
  }

  private fun addItemRow(
    index: Int,
    isDone: Boolean,
    text: String
  ) {
    val rowView = requireRowView(index)

    val checkBox = rowView.findViewById<CheckBox>(R.id.todo_done)
    val editText = rowView.findViewById<EditText>(R.id.todo_text)
    val deleteButton = rowView.findViewById<View>(R.id.todo_delete)

    checkBox.isChecked = isDone
    checkBox.setOnClickListener { onDoneClickedListener(index) }

    // Set the text on the Editable directly so we don't lose the cursor position.
    editText.text.replace(0, editText.text.length, text)

    if (isDone) {
      editText.text.setSpan(strikethroughSpan, 0, editText.text.length, 0)
    } else {
      editText.text.removeSpan(strikethroughSpan)
    }

    editText.setTextChangedListener { onTextChangedListener(index, it) }
    @Suppress("UsePropertyAccessSyntax")
    editText.setOnFocusChangeListener { _, hasFocus ->
      deleteButton.visibility = if (hasFocus) View.VISIBLE else View.GONE
    }

    deleteButton.visibility = if (editText.hasFocus()) View.VISIBLE else View.GONE
    deleteButton.setOnClickListener { onDeleteClickedListener(index) }
  }

  private fun pruneDeadRowsFrom(index: Int) {
    while (itemContainer.childCount > index) itemContainer.removeViewAt(index)
  }

  private fun requireRowView(index: Int): View {
    return if (index < itemContainer.childCount) {
      itemContainer.getChildAt(index)
    } else {
      inflater.inflate(R.layout.todo_item_layout, itemContainer, false)
          .also { row -> itemContainer.addView(row) }
    }
  }

  companion object {
    fun fromLinearLayout(itemContainer: LinearLayout): ItemListView {
      // We always want to restore view state from the workflow.
      itemContainer.isSaveFromParentEnabled = false
      return ItemListView(itemContainer)
    }
  }
}
