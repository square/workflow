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

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.squareup.sample.container.R
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.bindShowRendering
import com.squareup.workflow.ui.modal.ModalViewContainer

/**
 * Used by Tic Tac Workflow sample to show its [PanelContainerScreen]s.
 * Extends [ModalViewContainer] to make the dialog square on Tablets, and
 * give it an opaque background.
 */
class PanelContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : ModalViewContainer(context, attributeSet, defStyle, defStyleRes) {
  override fun buildDialogForView(view: View): Dialog {
    return Dialog(context, R.style.PanelDialog).also { dialog ->
      dialog.setContentView(view)

      val typedValue = TypedValue()
      context.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
      if (typedValue.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
        dialog.window!!.setBackgroundDrawable(ColorDrawable(typedValue.data))
      }

      // Use setLayout to control window size. Note that it must be
      // called after setContentView.
      //
      // Default layout values are MATCH_PARENT in both dimens, which is
      // perfect for phone.

      if (context.isTablet) {
        val displayMetrics = DisplayMetrics().also {
          dialog.context.display.getMetrics(it)
        }

        if (context.isPortrait) {
          dialog.window!!.setLayout(displayMetrics.widthPixels, displayMetrics.widthPixels)
        } else {
          dialog.window!!.setLayout(displayMetrics.heightPixels, displayMetrics.heightPixels)
        }
      }
    }
  }

  companion object : ViewFactory<PanelContainerScreen<*, *>> by BuilderBinding(
      type = PanelContainerScreen::class,
      viewConstructor = { initialRendering, initialHints, contextForNewView, _ ->
        PanelContainer(contextForNewView).apply {
          id = R.id.panel_container
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          bindShowRendering(initialRendering, initialHints, ::update)
        }
      }
  )
}
