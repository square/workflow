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
import com.squareup.sample.porchbox.InboxWorkflow.Output
import com.squareup.sample.porchbox.InboxWorkflow.Output.DropItem
import com.squareup.sample.porchbox.InboxWorkflow.Output.Exit
import com.squareup.sample.porchbox.InboxWorkflow.Output.Refresh
import com.squareup.sample.porchbox.InboxWorkflow.Output.Selection
import com.squareup.sample.porchbox.model.Inbox
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.makeEventSink

/**
 * Workflow that manages rendering the master list of [Item]s in the [Porch]. Receives refresh
 * and drop actions to send output to the [PorchWorkflow] for handling.
 */
object InboxWorkflow : StatelessWorkflow<Inbox, Output, InboxRendering>() {

  sealed class Output {
    class Selection(val selected: Int) : Output()
    object Exit : Output()
    object DropItem : Output()
    object Refresh: Output()
  }

  override fun render(
    props: Inbox,
    context: RenderContext<Nothing, Output>
  ): InboxRendering {
    val sink: Sink<Output> = context.makeEventSink { setOutput(it) }

    return InboxRendering(
        items = props.items.map { item ->
          ItemRowRendering(
              name = item.name,
              photoUri = item.photoUri
          )
        },
        onItemSelected = { sink.send(Selection(it)) },
        onExit = { sink.send(Exit) },
        onDropItem = { sink.send(DropItem) },
        onRefresh = { sink.send(Refresh) }
    )
  }
}

data class InboxRendering(
  val items: List<ItemRowRendering>,
  val onItemSelected: (Int) -> Unit,
  val onExit: () -> Unit,
  val onDropItem: () -> Unit,
  val onRefresh: () -> Unit,
  val selection: Int = -1
)

data class ItemRowRendering(
  val name: String,
  val photoUri: Uri?
)