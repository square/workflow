/*
 * Copyright 2018 Square Inc.
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
 */package com.squareup.sample.authgameapp

import android.app.AlertDialog
import android.content.Context
import android.view.View
import com.squareup.coordinators.Coordinator
import com.squareup.sample.tictactoe.ConfirmQuitScreen
import com.squareup.sample.tictactoe.RunGameEvent.ConfirmQuit
import com.squareup.sample.tictactoe.RunGameEvent.ContinuePlaying
import com.squareup.viewregistry.LayoutBinding
import com.squareup.viewregistry.StackedMainAndModalScreen
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.BackStackFrameLayout
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

/**
 * Manages the root view of [ShellActivity], and demonstrates using
 * an [AlertDialog] for modal duties (until we get around to something
 * more general purpose).
 *
 * Obviously in real life we would want
 * [com.squareup.sample.tictactoe.android.TicTacToeViewBuilders]
 * to provide its own binding for [ConfirmQuitScreen], as it does for
 * everything else. Getting there.
 */
class ShellCoordinator(
  private val screens: Observable<out StackedMainAndModalScreen<*, ConfirmQuitScreen>>,
  private val viewRegistry: ViewRegistry
) : Coordinator() {

  private val subs = CompositeDisposable()
  private var dialog: AlertDialog? = null

  override fun attach(view: View) {
    super.attach(view)

    val stackLayout = view.findViewById<BackStackFrameLayout>(R.id.view_stack)
    stackLayout.takeScreens(screens.map { mainAndModal -> mainAndModal.main }, viewRegistry)

    subs.add(screens.subscribe { screen ->
      with(screen.modals) {
        when {
          isEmpty() -> tearDownDialog()
          // Note that `single()` asserts that there is only one element in the list.
          // This container does not support nested modals.
          else -> ensureDialog(view.context, single())
        }
      }
    })
  }

  override fun detach(view: View) {
    subs.clear()

    // TODO(https://github.com/square/workflow/issues/51)
    // Not good enough, the stupid Activity cleans it up and shames us about "leaks" in logcat
    // before this point. Try to use a lifecycle observer to clean that up.
    tearDownDialog()

    super.detach(view)
  }

  private fun ensureDialog(
    context: Context,
    dialogScreen: ConfirmQuitScreen?
  ) {
    // Hacky, we no-op if the dialog is already showing. Good enough
    // because ConfirmQuitScreen has nothing to render.

    if (dialogScreen == null) {
      tearDownDialog()
    } else if (dialog == null) {
      dialog = AlertDialog.Builder(context)
          .setMessage("Do you really want to concede the game?")
          .setNegativeButton("No") { _, _ -> dialogScreen.onEvent(ContinuePlaying) }
          .setOnCancelListener { dialogScreen.onEvent(ContinuePlaying) }
          .setPositiveButton("I Quit") { _, _ -> dialogScreen.onEvent(ConfirmQuit) }
          .create()
          .apply { show() }
    }
  }

  private fun tearDownDialog() {
    dialog?.hide()
    dialog = null
  }

  companion object : ViewBinding<StackedMainAndModalScreen<*, ConfirmQuitScreen>>
  by LayoutBinding.of(R.layout.view_stack_layout, ::ShellCoordinator)
}
