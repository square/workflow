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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * Defines a type of editable value, including the [ViewHolder] to represent it in a `RecyclerView`
 * and functions for binding the holder.
 */
interface InputRow<T, VH : ViewHolder> {

  /**
   * Shown to the user when a prompted to add a new row.
   */
  val description: String

  /**
   * The initial value to use when showing a new row of this type.
   */
  val defaultValue: T

  /**
   * Creates a [ViewHolder] that knows how to render and edit values of type [T].
   */
  fun createViewHolder(parent: ViewGroup): VH

  /**
   * Update [holder] to show a new [value].
   */
  fun writeValue(
    holder: VH,
    value: T
  )

  /**
   * Replace the change listener on [holder].
   *
   * @param renderedValue The value that was last passed to [writeValue] for this row.
   */
  fun setListener(
    holder: VH,
    renderedValue: T,
    listener: (T) -> Unit
  )
}

val InputRow<*, *>.viewType: Int get() = this::class.hashCode()
