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
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.backPressedHandler
import com.squareup.workflow.ui.bindShowRendering
import com.squareup.workflow.ui.buildView
import com.squareup.workflow.ui.modal.ModalViewContainer.Companion.binding
import com.squareup.workflow.ui.onBackPressedDispatcherOwnerOrNull
import com.squareup.workflow.ui.showRendering
import kotlin.reflect.KClass

/**
 * Container that shows [HasModals.modals] as arbitrary views in a [Dialog]
 * window. Provides compatibility with [View.backPressedHandler].
 *
 * Use [binding] to assign particular rendering types to be shown this way.
 */
open class ModalViewContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : ModalContainer<Any>(context, attributeSet, defStyle, defStyleRes) {

  /**
   * Called from [buildDialog]. Builds (but does not show) the [Dialog] to
   * display a [view] built via [ViewRegistry].
   *
   * Subclasses may override completely to build their own kind of [Dialog],
   * there is no need to call `super`.
   */
  open fun buildDialogForView(view: View): Dialog {
    return Dialog(context).apply {
      setCancelable(false)
      setContentView(view)

      // Dialog is sized to wrap the view. Note that this call must come after
      // setContentView.
      window!!.setLayout(WRAP_CONTENT, WRAP_CONTENT)

      // If we don't set or clear the background drawable, the window cannot go full bleed.
      window!!.setBackgroundDrawable(null)
    }
  }

  final override fun buildDialog(
    initialModalRendering: Any,
    initialViewEnvironment: ViewEnvironment
  ): DialogRef<Any> {
    val view = initialViewEnvironment[ViewRegistry]
        .buildView(initialModalRendering, initialViewEnvironment, this)
        .apply {
          // If the modal's root view has no backPressedHandler, add a no-op one to
          // ensure that the `onBackPressed` call below will not leak up to handlers
          // that should be blocked by this modal session.
          if (backPressedHandler == null) backPressedHandler = { }
        }

    return buildDialogForView(view)
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
        }
        .run {
          DialogRef(initialModalRendering, initialViewEnvironment, this, view)
        }
  }

  override fun updateDialog(dialogRef: DialogRef<Any>) {
    with(dialogRef) { (extra as View).showRendering(modalRendering, viewEnvironment) }
  }

  @PublishedApi
  internal class Binding<H : HasModals<*, *>>(
    @IdRes id: Int,
    type: KClass<H>
  ) : ViewFactory<H>
  by BuilderBinding(
      type = type,
      viewConstructor = { initialRendering, initialHints, context, _ ->
        ModalViewContainer(context).apply {
          this.id = id
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          bindShowRendering(initialRendering, initialHints, ::update)
        }
      }
  )

  companion object {
    /**
     * Creates a [ViewFactory] for modal container screens of type [H].
     *
     * Each view created for [HasModals.modals] will be shown in a [Dialog]
     * whose window is set to size itself to `WRAP_CONTENT` (see [android.view.Window.setLayout]).
     *
     * @param id a unique identifier for containers of this type, allowing them to participate
     * view persistence
     */
    inline fun <reified H : HasModals<*, *>> binding(
      @IdRes id: Int = View.NO_ID
    ): ViewFactory<H> = Binding(id, H::class)
  }
}
