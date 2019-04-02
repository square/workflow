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
import android.support.annotation.IdRes
import android.support.annotation.StyleRes
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import io.reactivex.Observable
import kotlin.reflect.jvm.jvmName

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

  override fun Any.matches(nextModal: Any) = this::class.java == nextModal::class.java

  override fun showDialog(
    modalScreen: Any,
    screens: Observable<out Any>,
    viewRegistry: ViewRegistry
  ): Dialog {
    val binding = viewRegistry.getBinding<Any>(modalScreen::class.jvmName)
    val view = binding.buildView(screens, viewRegistry, this)

    return Dialog(context, dialogThemeResId).apply {
      setCancelable(false)
      setContentView(modalDecorator(view))
      window!!.setLayout(WRAP_CONTENT, WRAP_CONTENT)

      if (dialogThemeResId == 0) {
        // If we don't set or clear the background drawable, the window cannot go full bleed.
        window!!.setBackgroundDrawable(null)
      }
      show()
    }
  }

  class Binding<H : HasModals<*, *>>(
    @IdRes id: Int,
    type: Class<H>,
    @StyleRes dialogThemeResId: Int = 0,
    modalDecorator: (View) -> View = { it }
  ) : ViewBinding<H>
  by BuilderBinding(
      type = type,
      builder = { screens, viewRegistry, context, _ ->
        ModalViewContainer(
            context,
            modalDecorator = modalDecorator,
            dialogThemeResId = dialogThemeResId
        )
            .apply {
              this.id = id
              layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
              takeScreens(screens, viewRegistry)
            }
      }
  )
}
