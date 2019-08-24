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
internal class ModalViewContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  @StyleRes private val dialogThemeResId: Int = 0,
  private val modalDecorator: (View) -> View = { it }
) : ModalContainer<Any>(context, attributeSet) {

  override fun buildDialog(
    initialModalRendering: Any,
    viewRegistry: ViewRegistry
  ): DialogRef<Any> {
    val view = viewRegistry.buildView(initialModalRendering, this)

    return Dialog(context, dialogThemeResId)
        .apply {
          // TODO fix back button dispatch in modals. This doesn't terminate for some reason.
          // https://github.com/square/workflow/issues/466
//          setOnKeyListener { _, keyCode, _ ->
//            keyCode == KeyEvent.KEYCODE_BACK && HandlesBack.Helper.onBackPressed(view)
//          }

          setCancelable(false)
          setContentView(modalDecorator(view))
          window!!.setLayout(WRAP_CONTENT, WRAP_CONTENT)

          if (dialogThemeResId == 0) {
            // If we don't set or clear the background drawable, the window cannot go full bleed.
            window!!.setBackgroundDrawable(null)
          }
        }
        .run {
          DialogRef(initialModalRendering, this, view)
        }
  }

  override fun updateDialog(dialogRef: DialogRef<Any>) {
    with(dialogRef) { (extra as View).showRendering(modalRendering) }
  }

  class Binding<H : HasModals<*, *>>(
    @IdRes id: Int,
    type: KClass<H>,
    @StyleRes dialogThemeResId: Int = 0,
    modalDecorator: (View) -> View = { it }
  ) : ViewBinding<H>
  by BuilderBinding(
      type = type,
      viewConstructor = { viewRegistry, initialRendering, context, _ ->
        ModalViewContainer(
            context,
            modalDecorator = modalDecorator,
            dialogThemeResId = dialogThemeResId
        )
            .apply {
              this.id = id
              layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
              registry = viewRegistry
              bindShowRendering(initialRendering, ::update)
            }
      }
  )
}
