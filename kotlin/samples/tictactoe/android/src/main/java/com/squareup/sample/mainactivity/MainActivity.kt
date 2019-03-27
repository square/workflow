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
package com.squareup.sample.mainactivity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.squareup.sample.authworkflow.AuthViewBindings
import com.squareup.sample.gameworkflow.TicTacToeViewBindings
import com.squareup.sample.mainworkflow.RootScreen
import com.squareup.sample.panel.PanelContainer
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.viewregistry.HandlesBack
import com.squareup.viewregistry.ModalContainer
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.backstack.BackStackContainer
import com.squareup.viewregistry.backstack.PushPopEffect
import com.squareup.workflow.Snapshot
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlin.reflect.jvm.jvmName

/**
 * Prototype Android integration. Demonstrates:
 *
 * - preserving workflow state via the activity bundle
 * - simple two layer container, with body views and dialogs
 * - TODO: customizing stock views via wrapping (when we add a logout button to each game view)
 */
class MainActivity : AppCompatActivity() {
  private lateinit var component: MainComponent

  private lateinit var content: View

  private val subs = CompositeDisposable()

  private var latestSnapshot = Snapshot.EMPTY

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    component = lastCustomNonConfigurationInstance as? MainComponent
        ?: MainComponent()

    val snapshot = savedInstanceState?.getParcelable<ParceledSnapshot>(SNAPSHOT_NAME)
        ?.snapshot
    val updates = component.updates(snapshot)

    val screens = updates
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { latestSnapshot = it.snapshot }
        .map { it.rendering }

    val viewRegistry = buildViewRegistry()
    val rootViewBinding: ViewBinding<RootScreen> =
      viewRegistry.getBinding(AlertContainerScreen::class.jvmName)

    content = rootViewBinding.buildView(screens, viewRegistry, this)
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

  private fun buildViewRegistry(): ViewRegistry {
    return ViewRegistry(
        BackStackContainer,
        ModalContainer.forAlertContainerScreen(),
        PanelContainer
    ) + AuthViewBindings + TicTacToeViewBindings + PushPopEffect
  }

  private companion object {
    val SNAPSHOT_NAME = MainActivity::class.jvmName + "-snapshot"
  }
}
