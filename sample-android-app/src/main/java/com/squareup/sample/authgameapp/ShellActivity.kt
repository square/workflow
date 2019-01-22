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
import com.squareup.viewregistry.HandlesBack
import com.squareup.viewregistry.MainAndModalScreen
import com.squareup.viewregistry.StackedMainAndModalScreen
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.workflow.Snapshot
import com.squareup.workflow.rx2.state
import com.squareup.workflow.rx2.toCompletable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import java.io.File
import kotlin.coroutines.experimental.CoroutineContext
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
class ShellActivity : AppCompatActivity(), CoroutineScope {
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
  private val job = Job()

  private var latestSnapshot = Snapshot.EMPTY

  override val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.Main

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    component = lastCustomNonConfigurationInstance as? ShellComponent ?: ShellComponent()

    val initialState = savedInstanceState?.getParcelable<ParceledSnapshot>(SNAPSHOT_NAME)
        ?.snapshot?.bytes?.let { ShellState.fromSnapshot(it) }
        ?: ShellState.startingState()

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
    subs.add(workflow.toCompletable().subscribe { finish() })

    val viewFactory = buildViewFactory()
    val rootViewBinding: ViewBinding<StackedMainAndModalScreen<*, ConfirmQuitScreen>> =
      viewFactory[MainAndModalScreen::class.jvmName]

    content = rootViewBinding.buildView(screens, viewFactory, this)
        .apply { setContentView(this) }

    val traceFile = File(getExternalFilesDir(null), "workflow-trace.json")
    Timber.i("Writing traces to $traceFile every 5 seconds…")
    launch {
      while (true) {
        try {
          component.workflowTracer.writeTraceFile(traceFile)
          Timber.d("Wrote trace file.")
        } catch (e: Throwable) {
          Timber.w(e, "Failed to write trace file.")
        }
        delay(5000)
      }
    }

    with(component.workflowWebview) {
      start()
      Timber.d("Workflow inspector listening on port $port…")
    }
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
    job.cancel()
    component.workflowTracer.dispose()
    component.workflowWebview.stop()
    super.onDestroy()
  }

  override fun onRetainCustomNonConfigurationInstance(): Any = component

  private fun buildViewFactory(): ViewRegistry {
    return ViewRegistry(ShellCoordinator) + AuthViewBuilders + TicTacToeViewBuilders
  }

  private companion object {
    val SNAPSHOT_NAME = ShellActivity::class.jvmName + "-snapshot"
  }
}
