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

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.sample.porchbox.DropItemWorkflow.Action.Exit
import com.squareup.sample.porchbox.DropItemWorkflow.Action.Upload
import com.squareup.sample.porchbox.DropItemWorkflow.ExitDropItem
import com.squareup.sample.porchbox.DropItemWorkflow.Props
import com.squareup.sample.porchbox.DropItemWorkflow.State
import com.squareup.sample.porchbox.DropItemWorkflow.State.Empty
import com.squareup.sample.porchbox.DropItemWorkflow.State.Uploading
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Workflow that renders a screen to add the details for an item dropped at someone's [Porch].
 * Also manages the upload of this information to the backend.
 */
object DropItemWorkflow : StatefulWorkflow<Props, State, ExitDropItem, DropItemRendering>() {
  object Props

  sealed class State {
    object Empty : State()
    data class Uploading(
      val email: String,
      val name: String,
      val uri: String
    ) : State()
  }

  data class ExitDropItem(val success: Boolean)

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = Empty

  override fun render(
    props: Props,
    state: State,
    context: RenderContext<State, ExitDropItem>
  ): DropItemRendering = when (state) {
    Empty -> {
      val addSink = context.actionSink
      val onAddItem: ((email: String, name: String, uriString: String) -> Unit) =
        { email, name, uriString ->
          addSink.send(Upload(email, name, uriString))
        }
      DropItemRendering(
          onDropItem = onAddItem
      )
    }
    is Uploading -> {
      context.runningWorker(
          buildUploadWorker(state)
      ) {
        Exit(it)
      }
      DropItemRendering(
          onDropItem = { _, _, _ -> }
      )
    }
  }

  private fun buildUploadWorker(state: Uploading): Worker<Boolean> = Worker.create<Boolean> {
    val item = hashMapOf(
        "owner" to state.email,
        "name" to state.name,
        "photoUri" to state.uri
    )
    val db = Firebase.firestore
    val isSuccessful: Boolean = suspendCoroutine<Boolean> { cont ->
      db.collection("items")
          .add(item)
          .addOnCompleteListener {
            cont.resume(it.isSuccessful)
          }
    }
    emit(isSuccessful)
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private sealed class Action : WorkflowAction<State, ExitDropItem> {
    class Upload(
      val email: String,
      val name: String,
      val uri: String
    ) : Action()

    class Exit(val success: Boolean) : Action()

    override fun Updater<State, ExitDropItem>.apply() {
      when (this@Action) {
        is Upload -> nextState = Uploading(email, name, uri)
        is Exit -> setOutput(ExitDropItem(success = success))
      }
    }
  }
}

data class DropItemRendering(
  val onDropItem: ((String, String, String) -> Unit)
)