package com.squareup.viewregistry

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

val Context.isTablet: Boolean get() = resources.getBoolean(R.bool.is_tablet)

val Context.isPortrait: Boolean get() = resources.configuration.orientation == ORIENTATION_PORTRAIT

val Context.windowManager: WindowManager
  get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

val Context.display: Display get() = windowManager.defaultDisplay

val Context.displayMetrics: DisplayMetrics get() = DisplayMetrics().also { display.getMetrics(it) }
