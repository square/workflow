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

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.view.Display
import android.view.WindowManager
import com.squareup.sample.container.R

val Context.isPortrait: Boolean get() = resources.getBoolean(R.bool.is_portrait)

val Context.isTablet: Boolean get() = resources.getBoolean(R.bool.is_tablet)

val Context.windowManager: WindowManager get() = getSystemService(WINDOW_SERVICE) as WindowManager

val Context.display: Display get() = windowManager.defaultDisplay
