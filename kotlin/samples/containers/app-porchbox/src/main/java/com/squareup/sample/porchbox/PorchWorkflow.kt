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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.sample.container.masterdetail.MasterDetailScreen
import com.squareup.sample.porchbox.InboxWorkflow.Output.DropItem
import com.squareup.sample.porchbox.InboxWorkflow.Output.Exit
import com.squareup.sample.porchbox.InboxWorkflow.Output.Refresh
import com.squareup.sample.porchbox.InboxWorkflow.Output.Selection
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output.CloseItemList
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output.ShowNextItem
import com.squareup.sample.porchbox.ItemDetailWorkflow.Output.ShowPreviousItem
import com.squareup.sample.porchbox.PorchWorkflow.Action.ClearSelection
import com.squareup.sample.porchbox.PorchWorkflow.Action.ExitPorch
import com.squareup.sample.porchbox.PorchWorkflow.Action.HandleInboxDownload
import com.squareup.sample.porchbox.PorchWorkflow.Action.HandleInboxOutput
import com.squareup.sample.porchbox.PorchWorkflow.Action.LoadPorch
import com.squareup.sample.porchbox.PorchWorkflow.Action.Login
import com.squareup.sample.porchbox.PorchWorkflow.Action.SelectNext
import com.squareup.sample.porchbox.PorchWorkflow.Action.SelectPrevious
import com.squareup.sample.porchbox.PorchWorkflow.CloseInbox
import com.squareup.sample.porchbox.PorchWorkflow.Props
import com.squareup.sample.porchbox.PorchWorkflow.State
import com.squareup.sample.porchbox.PorchWorkflow.State.DroppingItem
import com.squareup.sample.porchbox.PorchWorkflow.State.PorchLoaded
import com.squareup.sample.porchbox.PorchWorkflow.State.LoadingPorch
import com.squareup.sample.porchbox.PorchWorkflow.State.LoggedOut
import com.squareup.sample.porchbox.PorchWorkflow.State.LoggingIn
import com.squareup.sample.porchbox.model.Inbox
import com.squareup.sample.porchbox.model.Item
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.ui.backstack.BackStackScreen
import com.squareup.workflow.ui.modal.AlertContainerScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Workflow to manage state for logging in and loading the Porch's data. This workflow also manages
 * the movement through the detail pages of the master/detail [Item] list.
 */
class PorchWorkflow(
  private val auth: Auth,
  private val db: RealtimeDB
) : StatefulWorkflow<Props, State, CloseInbox, AlertContainerScreen<Any>>() {
  object Props
  object CloseInbox

  /**
   * Very simple interface wrapper around FirebaseAuth to assist with test injection
   */
  interface Auth {
    val currentUser: FirebaseUser?
      get() = FirebaseAuth.getInstance().currentUser

    fun signInWithEmailAndPassword(
      email: String,
      pass: String,
      listener: OnCompleteListener<AuthResult>
    ): Task<AuthResult> {
      val auth = FirebaseAuth.getInstance()
      return auth.signInWithEmailAndPassword(email, pass)
          .addOnCompleteListener(listener)
    }
  }

  interface RealtimeDB {
    fun query(
      path: String,
      field: String? = null,
      subset: List<String?> = emptyList(),
      listener: OnSuccessListener<QuerySnapshot>
    ): Task<QuerySnapshot> {
      val db = Firebase.firestore
      return field?.let {
        db.collection(path)
            .whereIn(it, subset)
            .get()
            .addOnSuccessListener(listener)
      } ?: db.collection(path)
          .get()
          .addOnSuccessListener(listener)
    }
  }

  sealed class State {
    object LoggedOut : State()
    data class LoggingIn(
      val email: String,
      val pass: String
    ) : State()

    data class LoadingPorch(val user: FirebaseUser) : State()
    data class PorchLoaded(
      val user: FirebaseUser,
      val inbox: Inbox,
      val selected: Int
    ) : State()

    data class DroppingItem(
      val user: FirebaseUser,
      val inbox: Inbox,
      val selected: Int
    ) : State()
  }

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = LoggedOut

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun render(
    props: Props,
    state: State,
    context: RenderContext<State, CloseInbox>
  ): AlertContainerScreen<Any> = when (state) {
    LoggedOut -> {
      val loginScreen = context.renderChild(
          LoginWorkflow,
          LoginWorkflow.Props
      ) {
        Login(it.email, it.pass)
      }
      AlertContainerScreen(loginScreen)
    }
    is LoggingIn -> {
      context.runningWorker<FirebaseUser?>(
          buildLoginWorker(state)
      ) { user: FirebaseUser? ->
        user?.let {
          LoadPorch(it)
        } ?: ExitPorch
      }
      AlertContainerScreen(state)
    }

    is LoadingPorch -> {
      context.runningWorker<Inbox>(
          buildLoadingPorchWorker(state)
      ) { inbox: Inbox ->
        HandleInboxDownload(inbox)
      }
      AlertContainerScreen(state)
    }

    is PorchLoaded -> {
      val selected = state.selected
      val items = state.inbox.items
      val itemSelectedDetail =
        if (state.selected < 0) null
        else context.renderChild(
            ItemDetailWorkflow, ItemDetailWorkflow.Props(items[selected], selected, items.size),
            "$state"
        ) {
          when (it) {
            CloseItemList -> ClearSelection
            ShowPreviousItem -> SelectPrevious
            ShowNextItem -> SelectNext
          }
        }
            .let {
              BackStackScreen<Any>(it)
            }

      val itemsMasterList =
        context.renderChild(InboxWorkflow, state.inbox) { output ->
          when (output) {
            DropItem -> {
              Action.DropItem
            }
            Refresh -> {
              Action.LoadPorch(state.user)
            }
            is Selection -> {
              HandleInboxOutput(output.selected)
            }
            Exit -> {
              ExitPorch
            }
          }
        }
            .copy(selection = selected)
            .let { BackStackScreen<Any>(it) }

      itemSelectedDetail?.let {
        AlertContainerScreen<Any>(
            MasterDetailScreen(masterRendering = itemsMasterList, detailRendering = it)
        )
      } ?: AlertContainerScreen<Any>(
          MasterDetailScreen(masterRendering = itemsMasterList,
              selectDefault = { context.actionSink.send(HandleInboxOutput(0)) })
      )
    }

    is DroppingItem -> {
      val itemAdderScreen = context.renderChild(
          DropItemWorkflow,
          DropItemWorkflow.Props
      ) {
        if (it.success) {
          LoadPorch(state.user)
        } else {
          ExitPorch
        }
      }
      AlertContainerScreen(itemAdderScreen)
    }
  }

  private fun buildLoadingPorchWorker(state: LoadingPorch): Worker<Inbox> = Worker.create<Inbox> {
    val response: Inbox = suspendCoroutine { cont ->
      db.query(
          "items", "owner", listOf(state.user.email),
          OnSuccessListener<QuerySnapshot> { resultSnapshot ->
            cont.resume(
                Inbox(
                    resultSnapshot.documents.mapIndexed { index, itemDocument ->
                      Item(
                          itemDocument.getString("name") ?: "empty",
                          Uri.parse(itemDocument.getString("photoUri") ?: ""),
                          index
                      )
                    })
            )
          })
    }
    emit(response)
  }

  private fun buildLoginWorker(state: LoggingIn): Worker<FirebaseUser?> =
    Worker.create<FirebaseUser?> {
      val response: FirebaseUser? = suspendCoroutine<FirebaseUser?> { cont ->
        auth.signInWithEmailAndPassword(
            state.email, state.pass, OnCompleteListener<AuthResult> { task ->
          if (task.isSuccessful) {
            cont.resume(auth.currentUser)
          } else {
            cont.resume(null)
          }
        })
      }
      emit(response)
    }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private sealed class Action : WorkflowAction<State, CloseInbox> {
    class Login(
      val email: String,
      val pass: String
    ) : Action()

    class LoadPorch(val user: FirebaseUser) : Action()
    class HandleInboxDownload(val inbox: Inbox) : Action()
    object DropItem : Action()
    object ClearSelection : Action()
    object SelectPrevious : Action()
    object SelectNext : Action()
    class HandleInboxOutput(val selection: Int) : Action()
    object ExitPorch : Action()

    override fun Updater<State, CloseInbox>.apply() {
      when (this@Action) {
        is Login -> nextState = LoggingIn(email, pass)
        is LoadPorch -> nextState = LoadingPorch(user)
        ExitPorch -> setOutput(CloseInbox)
        else -> {
          when (nextState) {
            is LoadingPorch -> {
              val state = nextState as LoadingPorch
              when (this@Action) {
                is HandleInboxDownload -> {
                  nextState = PorchLoaded(
                      state.user,
                      inbox,
                      -1
                  )
                }
              }
            }
            is PorchLoaded -> {
              val state = nextState as PorchLoaded
              when (this@Action) {
                ClearSelection -> nextState = PorchLoaded(state.user, state.inbox, -1)
                SelectPrevious -> nextState =
                  PorchLoaded(state.user, state.inbox, state.selected - 1)
                SelectNext -> nextState = PorchLoaded(state.user, state.inbox, state.selected + 1)
                is HandleInboxOutput -> {
                  if (selection == -1) setOutput(CloseInbox)
                  nextState = PorchLoaded(state.user, state.inbox, selection)
                }
                DropItem -> nextState = DroppingItem(state.user, state.inbox, state.selected)
              }
            }
            else -> {
              setOutput(CloseInbox)
            }
          }
        }
      }
    }
  }
}