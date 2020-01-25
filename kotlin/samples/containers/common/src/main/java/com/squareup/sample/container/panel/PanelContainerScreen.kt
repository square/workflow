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
package com.squareup.sample.container.panel

import com.squareup.workflow.ui.backstack.BackStackScreen
import com.squareup.workflow.ui.modal.HasModals

/**
 * Custom modal container used in Tic Tac Workflow sample. Shows one or more
 * nested [sub-flows][modals] over a [baseScreen]. Demonstrates how an app
 * can set up a custom modal design element.
 *
 * Note the trickiness with our implementation of [HasModals], the interface
 * that drives Workflow's `ModalContainer`. We wrap the base in a [ScrimContainerScreen]
 * to give ourselves control over how the base is dimmed when the card modal is shown.
 *
 * Tic Tac Workflow uses modals for two purposes:
 *
 *  - Alerts, via the stock `AlertContainerScreen`
 *
 *  - Panels, this class: full screen (phone) or great big square (tablet)
 *    windows that host sub-tasks like logging in and choosing player names,
 *    tasks which take multiple steps and involve going backward and forward.
 */
data class PanelContainerScreen<B : Any, T : Any>(
  val baseScreen: B,
  override val modals: List<BackStackScreen<T>> = emptyList()
) : HasModals<ScrimContainerScreen<B>, BackStackScreen<T>> {
  override val beneathModals: ScrimContainerScreen<B>
    get() = ScrimContainerScreen(
        wrapped = baseScreen,
        dimmed = modals.isNotEmpty()
    )
}

/**
 * Shows the receiving [BackStackScreen] in the only panel over [baseScreen].
 */
fun <B : Any, T : Any> BackStackScreen<T>.inPanelOver(baseScreen: B): PanelContainerScreen<B, T> {
  return PanelContainerScreen(baseScreen, listOf(this))
}

/**
 * Shows the receiver as the only panel over [baseScreen], with no back stack.
 */
fun <B : Any, T : Any> T.firstInPanelOver(baseScreen: B): PanelContainerScreen<B, T> {
  return BackStackScreen(this, emptyList()).inPanelOver(baseScreen)
}
