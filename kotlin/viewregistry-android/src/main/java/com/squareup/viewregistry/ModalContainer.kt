package com.squareup.viewregistry

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import io.reactivex.Observable
import kotlin.reflect.jvm.jvmName

/**
 * Minimalist container view for [ModalContainerScreen].
 */
class ModalContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : AbstractModalContainer<Any>(context, attributeSet) {

  override fun Any.matches(nextModal: Any) = this::class.java == nextModal::class.java

  override fun showDialog(
    modalScreen: Any,
    screens: Observable<out Any>,
    viewRegistry: ViewRegistry
  ): Dialog {
    val binding = viewRegistry.getBinding<Any>(modalScreen::class.jvmName)
    val view = binding.buildView(screens, viewRegistry, this)

    return Dialog(context).apply {
      setCancelable(false)
      setContentView(view)
      window.setLayout(WRAP_CONTENT, WRAP_CONTENT)

      // If we don't set or clear the background drawable, the window cannot go full bleed.
      //
      // Another approach is to pass a theme to the Dialog constructor,
      // e.g. Dialog(context, android.R.style.Theme_Material_Panel). Themes can control
      // _everything_, including the animation used when showing or hiding the dialog window.
      window.setBackgroundDrawable(null)
      show()
    }
  }

  companion object : ViewBinding<ModalContainerScreen<*, *>>
  by BuilderBinding(
      type = ModalContainerScreen::class.java,
      builder = { screens, viewRegistry, context, _ ->
        ModalContainer(context)
            .apply {
              layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
              takeScreens(screens, viewRegistry)
            }
      }
  )
}
