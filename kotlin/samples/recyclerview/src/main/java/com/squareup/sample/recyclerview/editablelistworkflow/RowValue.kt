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

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.sample.recyclerview.inputrows.InputRow

/**
 * Holds an [InputRow] and the current value to display in that row.
 *
 * Also has [writeValue] and [setListener], which are convenience wrappers around the methods of
 * the same name on [InputRow] that get around type constraints and do the casting for you.
 */
data class RowValue<T, VH : ViewHolder>(
  val row: InputRow<T, VH>,
  val value: T
) {
  constructor(row: InputRow<T, VH>) : this(row, row.defaultValue)

  @Suppress("UNCHECKED_CAST")
  fun writeValue(
    holder: ViewHolder,
    newValue: Any?
  ) {
    row.writeValue(holder as VH, newValue as T)
  }

  @Suppress("UNCHECKED_CAST")
  fun setListener(
    holder: ViewHolder,
    renderedValue: Any?,
    listener: (T) -> Unit
  ) {
    row.setListener(holder as VH, renderedValue as T, listener)
  }

  @Suppress("UNCHECKED_CAST")
  fun withValue(newValue: Any?): RowValue<T, VH> = copy(value = newValue as T)
}
