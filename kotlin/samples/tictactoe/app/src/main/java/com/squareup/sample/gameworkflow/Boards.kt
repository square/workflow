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
package com.squareup.sample.gameworkflow

import android.view.ViewGroup
import android.widget.TextView
import com.squareup.sample.gameworkflow.Player.O
import com.squareup.sample.gameworkflow.Player.X

/**
 * Shared code for painting a 3 x 3 set of [TextView] cells with the values
 * of a [Board].
 */
internal fun Board.render(viewGroup: ViewGroup) {
  for (i in 0..8) {
    val row = i / 3
    val col = i % 3

    val cell = viewGroup.getChildAt(i) as TextView
    val box = this[row][col]
    cell.text = box?.symbol ?: ""
  }
}

val Player.symbol: String
  get() = when (this) {
    X -> "🙅"
    O -> "🙆"
  }
