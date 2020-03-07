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
package com.squareup.workflow.ui.modal

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.bindShowRendering
import com.squareup.workflow.ui.modal.AlertScreen.Button
import com.squareup.workflow.ui.modal.AlertScreen.Button.NEGATIVE
import com.squareup.workflow.ui.modal.AlertScreen.Button.NEUTRAL
import com.squareup.workflow.ui.modal.AlertScreen.Button.POSITIVE
import com.squareup.workflow.ui.modal.AlertScreen.Event.ButtonClicked
import com.squareup.workflow.ui.modal.AlertScreen.Event.Canceled

/**
 * Renders the [AlertScreen]s of an [AlertContainerScreen] as [AlertDialog]s.
 */
class AlertContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0,
  @StyleRes private val dialogThemeResId: Int = 0
) : ModalContainer<AlertScreen>(context, attributeSet, defStyle, defStyleRes) {

  override fun buildDialog(
    initialModalRendering: AlertScreen,
    initialViewEnvironment: ViewEnvironment
  ): DialogRef<AlertScreen> {
    val dialog = AlertDialog.Builder(context, dialogThemeResId)
        .create()
    val ref = DialogRef(initialModalRendering, initialViewEnvironment, dialog)
    updateDialog(ref)
    return ref
  }

  override fun updateDialog(dialogRef: DialogRef<AlertScreen>) {
    val dialog = dialogRef.dialog as AlertDialog
    val rendering = dialogRef.modalRendering

    if (rendering.cancelable) {
      dialog.setOnCancelListener { rendering.onEvent(Canceled) }
      dialog.setCancelable(true)
    } else {
      dialog.setCancelable(false)
    }

    for (button in Button.values()) {
      rendering.buttons[button]
          ?.let { name ->
            dialog.setButton(button.toId(), name) { _, _ ->
              rendering.onEvent(ButtonClicked(button))
            }
          }
          ?: run {
            dialog.getButton(button.toId())
                ?.visibility = View.INVISIBLE
          }
    }

    dialog.setMessage(rendering.message)
    dialog.setTitle(rendering.title)
  }

  private fun Button.toId(): Int = when (this) {
    POSITIVE -> DialogInterface.BUTTON_POSITIVE
    NEGATIVE -> DialogInterface.BUTTON_NEGATIVE
    NEUTRAL -> DialogInterface.BUTTON_NEUTRAL
  }

  private class AlertContainerBinding(
    @StyleRes private val dialogThemeResId: Int = 0
  ) : ViewFactory<AlertContainerScreen<*>> by BuilderBinding(
      type = AlertContainerScreen::class,
      viewConstructor = { initialRendering, initialHints, context, _ ->
        AlertContainer(context, dialogThemeResId = dialogThemeResId)
            .apply {
              id = R.id.workflow_alert_container
              layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
              bindShowRendering(initialRendering, initialHints, ::update)
            }
      }
  )

  companion object : ViewFactory<AlertContainerScreen<*>> by AlertContainerBinding() {
    /**
     * Creates a [ViewFactory] to show the [AlertScreen]s of an [AlertContainerScreen]
     * as Android `AlertDialog`s.
     *
     * @param dialogThemeResId the resource ID of the theme against which to inflate
     * dialogs. Defaults to `0` to use the parent `context`'s default alert dialog theme.
     */
    fun customThemeBinding(
      @StyleRes dialogThemeResId: Int = 0
    ): ViewFactory<AlertContainerScreen<*>> = AlertContainerBinding(dialogThemeResId)
  }
}
