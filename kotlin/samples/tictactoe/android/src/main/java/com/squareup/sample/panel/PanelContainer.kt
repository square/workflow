package com.squareup.sample.panel

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.FrameLayout
import com.squareup.sample.mainworkflow.display
import com.squareup.sample.mainworkflow.isTablet
import com.squareup.sample.tictactoe.R
import com.squareup.viewregistry.BackStackScreen
import com.squareup.viewregistry.BuilderBinding
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.buildView
import com.squareup.viewregistry.takeWhileAttached
import io.reactivex.Observable
import kotlin.math.min
import kotlin.reflect.jvm.jvmName

/**
 * [FrameLayout] that calculates its size based on the screen size -- to fill the screen on
 * phones, or make a square based on the shorter screen dimension on tablets. Expectation
 * is that this view will be shown in a `Dialog` window that is set to `WRAP_CONTENT`.
 *
 * Looks suspiciously like the modal flow container in Square PoS.
 */
class PanelContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
  init {
    background = ColorDrawable(resources.getColor(R.color.panelBody))
  }

  /** For use only by [onMeasure]. Instantiated here to avoid allocation during measure. */
  private val displayMetrics = DisplayMetrics()

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    context.display.getMetrics(displayMetrics)
    val calculatedWidthSpec: Int
    val calculatedHeightSpec: Int

    if (context.isTablet) {
      val size = min(displayMetrics.widthPixels, displayMetrics.heightPixels)

      calculatedWidthSpec = makeMeasureSpec(size, EXACTLY)
      calculatedHeightSpec = makeMeasureSpec(size, EXACTLY)
    } else {
      calculatedWidthSpec = makeMeasureSpec(displayMetrics.widthPixels, EXACTLY)
      calculatedHeightSpec = makeMeasureSpec(displayMetrics.heightPixels, EXACTLY)
    }

    super.onMeasure(calculatedWidthSpec, calculatedHeightSpec)
  }

  fun takeScreens(
    screens: Observable<out PanelScreen<*>>,
    viewRegistry: ViewRegistry
  ) {
    val binding = viewRegistry.getBinding<BackStackScreen<*>>(BackStackScreen::class.jvmName)

    takeWhileAttached(screens.firstElement().toObservable()) {
      // We only need to build a view for the first BackStackScreen, because that view
      // can display all succeeding ones.
      addView(
          binding.buildView(
              screens.map { it.backStackScreen },
              viewRegistry,
              this
          )
      )
    }
  }

  companion object : ViewBinding<PanelScreen<*>>
  by BuilderBinding(
      type = PanelScreen::class.java,
      builder = { screens, viewRegistry, context, _ ->
        PanelContainer(context).apply { takeScreens(screens, viewRegistry) }
      }
  )
}
