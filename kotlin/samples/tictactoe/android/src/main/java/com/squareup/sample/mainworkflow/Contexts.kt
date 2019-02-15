package com.squareup.sample.mainworkflow

import android.content.Context
import android.view.Display
import android.view.WindowManager
import com.squareup.sample.tictactoe.R

val Context.isTablet: Boolean get() = resources.getBoolean(R.bool.is_tablet)

val Context.windowManager: WindowManager
  get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

val Context.display: Display get() = windowManager.defaultDisplay
