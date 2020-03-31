/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.porchbox.view

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.cycler.DataSource
import com.squareup.cycler.ItemComparator
import com.squareup.cycler.Recycler
import com.squareup.picasso.Picasso
import com.squareup.sample.porchbox.InboxRendering
import com.squareup.sample.porchbox.ItemRowRendering
import com.squareup.sample.porchbox.R
import com.squareup.sample.porchbox.R.id
import com.squareup.sample.porchbox.R.layout
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory

/**
 * Renders the [RecyclerView] for the master list of [Item]s using the
 * [Cycler](https://github.com/square/cycler) library.
 */
class InboxLayoutRunner(rootView: View) : LayoutRunner<InboxRendering> {

  private data class ItemWrapper(
    val item: ItemRowRendering,
    val onClicked: () -> Unit
  )

  private val recycler =
    Recycler.adopt<ItemWrapper>(
        rootView.findViewById(
            id.inbox_recycler
        )
    ) {
      itemComparator = object : ItemComparator<ItemWrapper> {
        override fun areSameContent(
          oldItem: ItemWrapper,
          newItem: ItemWrapper
        ): Boolean {
          return oldItem.item.name == newItem.item.name
        }

        override fun areSameIdentity(
          oldItem: ItemWrapper,
          newItem: ItemWrapper
        ): Boolean {
          return oldItem.item.name == newItem.item.name
              && oldItem.item.photoUri == newItem.item.photoUri
        }
      }
      // This defines how to instantiate and bind a row of a particular item type.
      row<ItemWrapper, ViewGroup> {
        // This defines how to inflate and bind the layout for this row type.
        create(layout.item_list_item) {
          // And this is the actual binding logic.
          bind { _, item ->
            val card: CardView = view.findViewById(
                id.item_card
            )
            val itemNameView: TextView = view.findViewById(
                id.item_name
            )
            val itemPhotoPreview: ImageView = view.findViewById(
                id.item_preview
            )

            // Because we have a [ItemComparator] bindings will only happen on diff.
            Picasso.get()
                .load(item.item.photoUri)
                .resizeDimen(R.dimen.preview_size, R.dimen.preview_size)
                .into(itemPhotoPreview)

            itemNameView.text = item.item.name
            card.setOnClickListener { item.onClicked() }
          }
        }
      }
    }

  private val addItemButton =
    rootView.findViewById<FloatingActionButton>(R.id.inbox_drop_item_button)
  private val refreshButton = rootView.findViewById<FloatingActionButton>(R.id.inbox_refresh_button)

  override fun showRendering(
    rendering: InboxRendering,
    viewEnvironment: ViewEnvironment
  ) {
    recycler.data = rendering.toDataSource()

    addItemButton.setOnClickListener { rendering.onDropItem() }
    refreshButton.setOnClickListener { rendering.onRefresh() }
  }

  private fun InboxRendering.toDataSource(): DataSource<ItemWrapper> =
    object : DataSource<ItemWrapper> {
      override val size: Int
        get() = items.size

      override fun get(i: Int): ItemWrapper =
        ItemWrapper(
            item = items[i],
            onClicked = { onItemSelected(i) }
        )
    }

  companion object : ViewFactory<InboxRendering> by bind(
      layout.inbox_layout, ::InboxLayoutRunner
  )
}