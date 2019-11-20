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

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import kotlin.reflect.KClass

/**
 * Class returned by [ModalContainer.forContainerScreen], qv for details.
 */
@PublishedApi
internal class ModalViewContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0,
  @StyleRes private val dialogThemeResId: Int = 0,
  private val modalDecorator: (View) -> View = { it }
) : ModalContainer<Any>(context, attributeSet, defStyle, defStyleRes) {
  override fun buildDialog(
    initialModalRendering: Any,
    initialContainerHints: ContainerHints
  ): DialogRef<Any> {
    val view = initialContainerHints[ViewRegistry].buildView(
        initialModalRendering, initialContainerHints, this
    )

    return Dialog(context, dialogThemeResId)
        .apply {
          // Dialogs are modal windows and so they block events, including back button presses
          // -- that's their job! But we *want* the Activity's onBackPressedDispatcher to fire
          // when back is pressed, so long as it doesn't look past this modal window for handlers.
          //
          // Here, we handle the ACTION_UP portion of a KEYCODE_BACK key event, and below
          // we make sure that the root view has a backPressedHandler that will consume the
          // onBackPressed call if no child of the root modal view does.

          setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == ACTION_UP) {
              view.context.onBackPressedDispatcherOwnerOrNull()
                  ?.onBackPressedDispatcher
                  ?.let {
                    if (it.hasEnabledCallbacks()) it.onBackPressed()
                  }
              true
            } else {
              false
            }
          }

          setCancelable(false)
          setContentView(modalDecorator(view))
              .apply {
                // If the modal's root view has no backPressedHandler, add a no-op one to
                // ensure that the `onBackPressed` call above will not leak up to handlers
                // that should be blocked by this modal session.
                if (backPressedHandler == null) backPressedHandler = { }
              }

          window!!.setLayout(WRAP_CONTENT, WRAP_CONTENT)

          if (dialogThemeResId == 0) {
            // If we don't set or clear the background drawable, the window cannot go full bleed.
            window!!.setBackgroundDrawable(null)
          }
        }
        .run {
          DialogRef(initialModalRendering, initialContainerHints, this, view)
        }
  }

  override fun updateDialog(dialogRef: DialogRef<Any>) {
    with(dialogRef) { (extra as View).showRendering(modalRendering, containerHints) }
  }

  class Binding<H : HasModals<*, *>>(
    @IdRes id: Int,
    type: KClass<H>,
    @StyleRes dialogThemeResId: Int = 0,
    modalDecorator: (View) -> View = { it }
  ) : ViewBinding<H>
  by BuilderBinding(
      type = type,
      viewConstructor = { initialRendering, initialHints, context, _ ->
        ModalViewContainer(
            context,
            modalDecorator = modalDecorator,
            dialogThemeResId = dialogThemeResId
        ).apply {
          this.id = id
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          bindShowRendering(initialRendering, initialHints, ::update)
        }
      }
  )
}
