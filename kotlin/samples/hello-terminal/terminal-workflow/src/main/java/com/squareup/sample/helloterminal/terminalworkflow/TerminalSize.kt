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

import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.TerminalResizeListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import com.googlecode.lanterna.TerminalSize as LanternaTerminalSize

/**
 * Indicates the size of the terminal.
 *
 * The input type for [TerminalWorkflow].
 */
data class TerminalSize(
  val rows: Int,
  val columns: Int
)

@UseExperimental(ExperimentalCoroutinesApi::class)
internal fun Terminal.resizes(): Flow<TerminalSize> = channelFlow<TerminalSize> {
  val resizeListener = TerminalResizeListener { _, newSize -> offer(newSize.toSize()) }
  addResizeListener(resizeListener)
  awaitClose { removeResizeListener(resizeListener) }
}.conflate()

internal fun LanternaTerminalSize.toSize(): TerminalSize = TerminalSize(rows, columns)
