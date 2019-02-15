package com.squareup.sample.panel

import com.squareup.viewregistry.BackStackScreen
import com.squareup.viewregistry.ModalContainerScreen

/**
 * Shows an optional stack of modal [PanelScreen]s over a base screen.
 */
typealias PanelContainerScreen<B, T> = ModalContainerScreen<B, PanelScreen<T>>

/**
 * Wraps a screen of a modal subflow in the Tic Tac Workflow sample app.
 */
data class PanelScreen<out T : Any>(
  val backStackScreen: BackStackScreen<T>
)

fun <B: Any, T: Any> T.asPanelOver(base: B) : PanelContainerScreen<B, T> {
  return ModalContainerScreen(base, PanelScreen(BackStackScreen(this)))
}

fun <B: Any, T: Any> BackStackScreen<T>.asPanelOver(base: B):  PanelContainerScreen<B, T> {
  return ModalContainerScreen(base, PanelScreen(this))
}
