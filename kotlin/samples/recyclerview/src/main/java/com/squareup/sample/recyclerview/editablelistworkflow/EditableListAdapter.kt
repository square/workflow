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

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.Rendering
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.Asynchronous
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.None
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.Synchronous
import com.squareup.sample.recyclerview.inputrows.viewType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class EditableListAdapter : Adapter<ViewHolder>() {

  private val scope = CoroutineScope(Dispatchers.Main)
  private var diffJob: Job = Job()
  private var rendering: Rendering? = null
  private val rows: List<RowValue<*, *>> get() = checkNotNull(rendering?.rowValues)

  fun updateRendering(
    newRendering: Rendering,
    diffMode: ListDiffMode
  ) {
    diffJob.cancel()
    Timber.i("Updating adapter list: $diffMode")
    when (diffMode) {
      None -> {
        rendering = newRendering
        notifyDataSetChanged()
      }
      Synchronous -> {
        val diffCallback = DiffCallback(rendering, newRendering)
        val diffResult = DiffUtil.calculateDiff(diffCallback, false)
        rendering = newRendering
        diffResult.dispatchUpdatesTo(this@EditableListAdapter)
      }
      Asynchronous -> {
        val diffCallback = DiffCallback(rendering, newRendering)
        diffJob = scope.launch {
          val diffResult = withContext(Dispatchers.Default) {
            Timber.i("Starting async diff")
            // Fake delay to simulate long diff calculation.
            delay(500)
            DiffUtil.calculateDiff(diffCallback, false)
          }
          Timber.i("Async diff ready")
          rendering = newRendering
          diffResult.dispatchUpdatesTo(this@EditableListAdapter)
        }
      }
    }
  }

  override fun getItemCount(): Int = rendering?.rowValues?.size ?: 0

  /**
   * Items are always in a fixed order.
   */
  override fun getItemId(position: Int): Long = position.toLong()

  /**
   * Every position is a different type.
   */
  override fun getItemViewType(position: Int): Int = rows[position].row.viewType

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    Timber.i("Creating new ViewHolder for type $viewType…")
    return rows.first { it.row.viewType == viewType }
        .row.createViewHolder(parent)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    Timber.i("Binding $position to $holder…")
    rendering?.let { rendering ->
      val rowValue = rendering.rowValues[position]
      // Disable the listener first, so we don't try sending view events while we're updating
      // values, which can be invalid, for example, during scrolling.
      rowValue.setListener(holder, rowValue.value) {}
      rowValue.writeValue(holder, rowValue.value)
      rowValue.setListener(holder, rowValue.value) { rendering.onValueChanged(position, it) }
    }
  }
}

private class DiffCallback(
  private val oldRendering: Rendering?,
  private val newRendering: Rendering?
) : DiffUtil.Callback() {
  override fun areItemsTheSame(
    oldItemPosition: Int,
    newItemPosition: Int
  ): Boolean = oldItemPosition == newItemPosition

  override fun areContentsTheSame(
    oldItemPosition: Int,
    newItemPosition: Int
  ): Boolean {
    // Note: if you can't turn off item animations, you need to return false here for
    // items whose contents (not positions) have changed (e.g. oldPos == newPos).
    val oldItem = oldRendering?.rowValues?.get(oldItemPosition)
    val newItem = newRendering?.rowValues?.get(newItemPosition)
    return oldItem == newItem
  }

  override fun getOldListSize(): Int = oldRendering?.rowValues?.size ?: 0
  override fun getNewListSize(): Int = newRendering?.rowValues?.size ?: 0
}
