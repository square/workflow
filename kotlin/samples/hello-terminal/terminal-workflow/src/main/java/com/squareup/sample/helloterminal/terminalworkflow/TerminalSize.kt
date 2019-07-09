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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

/**
 * A [Flow] of the current terminal size. Will always emit the current size immediately on
 * collection.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
internal val Terminal.size: Flow<TerminalSize>
  get() = channelFlow<TerminalSize> {
    val resizeListener = TerminalResizeListener { _, newSize -> offer(newSize.toSize()) }
    addResizeListener(resizeListener)
    awaitClose { removeResizeListener(resizeListener) }
  }
      // Conflate first, so it fuses with the channelFlow operator.
      .conflate()
      .startWith(terminalSize.toSize())

internal fun LanternaTerminalSize.toSize(): TerminalSize = TerminalSize(rows, columns)

@UseExperimental(ExperimentalCoroutinesApi::class)
private fun <T> Flow<T>.startWith(value: T): Flow<T> = flow {
  emit(value)
  emitAll(this@startWith)
}
