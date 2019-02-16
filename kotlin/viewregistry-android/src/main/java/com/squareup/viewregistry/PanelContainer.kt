package com.squareup.viewregistry

import android.R.style
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import com.squareup.viewregistry.R.layout
import io.reactivex.Observable
import kotlin.math.min
import kotlin.reflect.jvm.jvmName

/**
 * Container view for [PanelContainerScreen].
 */
class PanelContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : AbstractModalContainer<BackStackScreen<*>>(context, attributeSet) {

  /**
   * Always returns `true`, because a view created to display one [BackStackScreen]
   * can display any [BackStackScreen].
   */
  override fun BackStackScreen<*>.matches(modalScreen: BackStackScreen<*>) = true

  override fun showDialog(
    modalScreen: BackStackScreen<*>,
    screens: Observable<out BackStackScreen<*>>,
    viewRegistry: ViewRegistry
  ): Dialog {
    val binding = viewRegistry.getBinding<BackStackScreen<*>>(modalScreen::class.jvmName)
    val view = binding.buildView(screens, viewRegistry, this)

    // TODO Could we instead define a theme based on, say, style.Theme_Light,
    // to define *everything*, including entry and exit animation? e.g., this
    // was working nicely except that I couldn't make the window background transparent.
    // Didn't try hard, though. Probably would still have to use code to force body
    // width / height, though, since that needs to be based on screen geometry, regardless
    // of what shape the window is forced into by the soft keyboard.
    /*
    return Dialog(context, style.Theme_Light).apply {
      setContentView(layout.panel_container)
      findViewById<ViewGroup>(R.id.panelBody).addView(view)
      show()
      window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    */

    return Dialog(context).apply {
      setContentView(layout.panel_container)
      setCancelable(false)
      val body = findViewById<ViewGroup>(R.id.panelBody)

      if (context.isTablet) {
        val displayMetrics = context.displayMetrics
        val size = min(displayMetrics.widthPixels, displayMetrics.heightPixels)

        if (context.isPortrait) {
          body.layoutParams.height = size
        } else {
          body.layoutParams.width = size
        }
      }

      body.addView(view)
      window.setLayout(MATCH_PARENT, MATCH_PARENT)
      window.clearFlags(FLAG_DIM_BEHIND)
      window.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.PANEL_SCRIM)))
      show()
    }
  }

  companion object : ViewBinding<PanelContainerScreen<*>>
  by BuilderBinding(
      type = PanelContainerScreen::class.java,
      builder = { screens, viewRegistry, context, _ ->
        PanelContainer(context).apply {
          layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
          takeScreens(screens, viewRegistry)
        }
      }
  )
}
