/*
 * Copyright 2017 Square Inc.
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
package com.squareup.sample.authgameapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.squareup.sample.authworkflow.android.AuthViewBuilders
import com.squareup.sample.tictactoe.ConfirmQuitScreen
import com.squareup.sample.tictactoe.android.TicTacToeViewBuilders
import com.squareup.viewbuilder.HandlesBack
import com.squareup.viewbuilder.MainAndModalScreen
import com.squareup.viewbuilder.StackedMainAndModalScreen
import com.squareup.viewbuilder.ViewBuilder
import com.squareup.viewbuilder.ViewBuilder.Registry
import com.squareup.workflow.Snapshot
import com.squareup.workflow.rx2.result
import com.squareup.workflow.rx2.state
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import kotlin.reflect.jvm.jvmName

/**
 * Prototype Android integration. Demonstrates:
 *
 * - preserving workflow state via the activity bundle
 * - simple two layer container, with body views and dialogs
 * - customizing stock views via wrapping (when we add a logout button
 *   to each game view)
 *
 */
class ShellActivity : AppCompatActivity() {
  private lateinit var component: ShellComponent

  /** Workflow decides what we're doing. */
  private lateinit var workflow: ShellWorkflow

  /**
   * TODO(ray) Weird interim state, bad example: IRL this would be
   * something like `Observable<out StackedMainAndModalScreen<*, *>>`,
   * but dialog support is nascent.
   *
   * More interesting apps have richer, bespoke root screens, to handle
   * things like their specific status bars, menu drawers, whatever.
   */
  private lateinit var screens: Observable<out StackedMainAndModalScreen<*, ConfirmQuitScreen>>
  private lateinit var content: View

  private val subs = CompositeDisposable()

  private var latestSnapshot = Snapshot.EMPTY

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    component = lastCustomNonConfigurationInstance as? ShellComponent ?: ShellComponent()

    val snapshot = savedInstanceState
        ?.getParcelable<ParceledSnapshot>(SNAPSHOT_NAME)
        ?.snapshot

    val initialState = ShellState.start(snapshot)
    workflow = component.shellReactor()
        .launch(initialState, component.workflowPool)

    screens = workflow.state
        .doOnNext {
          latestSnapshot = it.toSnapshot()
          Timber.d("showing: %s", it)
        }
        .map { state -> ShellRenderer.render(state, workflow, component.workflowPool) }

    // When the workflow fires its one and only result, quit the app.
    // TODO(ray) this never happens, add back button handling.
    subs.add(workflow.result.subscribe { finish() })

    val viewFactory = buildViewFactory()
    val rootViewBuilder: ViewBuilder<StackedMainAndModalScreen<*, ConfirmQuitScreen>> =
      viewFactory[MainAndModalScreen::class.jvmName]

    content = rootViewBuilder.buildView(screens, viewFactory, this)
        .apply { setContentView(this) }
  }

  override fun onBackPressed() {
    if (!HandlesBack.Helper.onBackPressed(content)) super.onBackPressed()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(SNAPSHOT_NAME, ParceledSnapshot(latestSnapshot))
  }

  override fun onDestroy() {
    subs.clear()
    super.onDestroy()
  }

  override fun onRetainCustomNonConfigurationInstance(): Any = component

  private fun buildViewFactory(): ViewBuilder.Registry {
    return Registry(ShellCoordinator) + AuthViewBuilders + TicTacToeViewBuilders
  }

  private companion object {
    val SNAPSHOT_NAME = ShellActivity::class.jvmName + "-snapshot"
  }
}
