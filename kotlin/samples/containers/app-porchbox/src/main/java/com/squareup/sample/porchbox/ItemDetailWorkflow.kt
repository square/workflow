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
package com.squareup.sample.porchbox

import android.net.Uri
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output.CloseItemList
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output.ShowNextItem
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output.ShowPreviousItem
import com.squareup.sample.porchbox.ItemDetailWorkflow.Props
import com.squareup.sample.porchbox.model.Item
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.makeEventSink
import com.squareup.workflow.ui.Compatible

/**
 * Simple Workflow to manage rendering and receiving the actions from the [Item] detail pages.
 * Passes these Actions back to [PorchWorkflow] to manage the master/detail screens.
 */
object ItemDetailWorkflow : StatelessWorkflow<Props, Output, ItemRendering>() {
  data class Props(
    val item: Item,
    val index: Int,
    val size: Int
  )

  /**
   * Possible actions sent back to [PorchWorkflow] from this Workflow.
   */
  enum class Output {
    CloseItemList,
    ShowPreviousItem,
    ShowNextItem
  }

  override fun render(
    props: Props,
    context: RenderContext<Nothing, Output>
  ): ItemRendering {
    val sink: Sink<Output> = context.makeEventSink { setOutput(it) }

    val onGoBack: (() -> Unit)? = when (props.index) {
      0 -> null
      else -> {
        { sink.send(ShowPreviousItem) }
      }
    }

    val onGoForth: (() -> Unit)? = when (props.index) {
      props.size - 1 -> null
      else -> {
        { sink.send(ShowNextItem) }
      }
    }

    return ItemRendering(
        name = props.item.name,
        photoUri = props.item.photoUri,
        onGoUp = { sink.send(CloseItemList) },
        onGoBack = onGoBack,
        onGoForth = onGoForth
    )
  }
}

data class ItemRendering(
  val name: String,
  val photoUri: Uri?,
  val onGoUp: () -> Unit,
  val onGoBack: (() -> Unit)? = null,
  val onGoForth: (() -> Unit)? = null
) : Compatible {
  override val compatibilityKey = name
}