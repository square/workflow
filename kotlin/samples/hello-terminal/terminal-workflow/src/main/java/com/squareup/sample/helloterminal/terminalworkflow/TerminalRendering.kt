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
package com.squareup.sample.helloterminal.terminalworkflow

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.TextColor.ANSI
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.BLACK
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.BLUE
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.CYAN
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.DEFAULT
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.GREEN
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.MAGENTA
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.RED
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.WHITE
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.YELLOW

/**
 * Represents the complete text to display on a terminal (is not additive), properties of that text,
 * as well as the event handler to handle keystrokes.
 *
 * The rendering type for [TerminalWorkflow].
 *
 * @param text The text to display to the terminal.
 * @param textColor Color of the text to display. All text will be the same color.
 * @param backgroundColor Color of the background of the terminal.
 */
data class TerminalRendering(
  val text: String,
  val textColor: Color = DEFAULT,
  val backgroundColor: Color = DEFAULT
) {
  enum class Color {
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE,
    DEFAULT
  }
}

internal fun Color.toTextColor(): TextColor = when (this) {
  BLACK -> ANSI.BLACK
  RED -> ANSI.RED
  GREEN -> ANSI.GREEN
  YELLOW -> ANSI.YELLOW
  BLUE -> ANSI.BLUE
  MAGENTA -> ANSI.MAGENTA
  CYAN -> ANSI.CYAN
  WHITE -> ANSI.WHITE
  DEFAULT -> ANSI.DEFAULT
}
