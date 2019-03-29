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
package com.squareup.workflow.ui

import android.content.Context
import android.support.annotation.StyleRes
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.squareup.workflow.ui.AlertScreen.Button.NEGATIVE
import com.squareup.workflow.ui.AlertScreen.Button.NEUTRAL
import com.squareup.workflow.ui.AlertScreen.Button.POSITIVE
import com.squareup.workflow.ui.AlertScreen.Event.ButtonClicked
import com.squareup.workflow.ui.AlertScreen.Event.Canceled
import io.reactivex.Observable

/**
 * Class returned by [ModalContainer.forAlertContainerScreen], qv for details.
 */
internal class AlertContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  @StyleRes private val dialogThemeResId: Int = 0
) : ModalContainer<AlertScreen>(context, attributeSet) {

  override fun showDialog(
    modalScreen: AlertScreen,
    screens: Observable<out AlertScreen>,
    viewRegistry: ViewRegistry
  ): AlertDialog {
    val builder = AlertDialog.Builder(context, dialogThemeResId)

    if (modalScreen.cancelable) {
      builder.setOnCancelListener { modalScreen.onEvent(Canceled) }
    } else {
      builder.setCancelable(false)
    }

    for ((button, name) in modalScreen.buttons) {
      when (button) {
        POSITIVE -> builder.setPositiveButton(name) { _, _ ->
          modalScreen.onEvent(ButtonClicked(POSITIVE))
        }
        NEGATIVE -> builder.setNegativeButton(name) { _, _ ->
          modalScreen.onEvent(ButtonClicked(NEGATIVE))
        }
        NEUTRAL -> builder.setNeutralButton(name) { _, _ ->
          modalScreen.onEvent(ButtonClicked(NEUTRAL))
        }
      }
    }

    modalScreen.message.takeIf { it.isNotBlank() }
        .let { builder.setMessage(it) }
    modalScreen.title.takeIf { it.isNotBlank() }
        .let { builder.setTitle(it) }

    return builder.show()
  }

  class Binding(
    @StyleRes private val dialogThemeResId: Int = 0
  ) : ViewBinding<AlertContainerScreen<*>>
  by BuilderBinding(
      type = AlertContainerScreen::class.java,
      builder = { screens, viewRegistry, context, _ ->
        AlertContainer(context, dialogThemeResId = dialogThemeResId)
            .apply {
              id = R.id.workflow_alert_container
              layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
              takeScreens(screens, viewRegistry)
            }
      }
  )
}
