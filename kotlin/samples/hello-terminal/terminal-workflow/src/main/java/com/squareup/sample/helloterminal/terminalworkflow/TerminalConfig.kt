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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.TerminalResizeListener
import com.squareup.sample.helloterminal.terminalworkflow.TerminalConfig.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * TODO kdoc
 */
data class TerminalConfig(
  val size: Size
) {
  data class Size(
    val rows: Int,
    val columns: Int
  )
}

internal fun TerminalSize.toSize(): Size = Size(rows, columns)

@UseExperimental(ExperimentalCoroutinesApi::class)
internal fun Terminal.listenForResizesOn(scope: CoroutineScope): ReceiveChannel<TerminalSize> =
  scope.produce(capacity = Channel.CONFLATED) {
    val resizeListener = TerminalResizeListener { _, newSize -> offer(newSize) }
    invokeOnClose { removeResizeListener(resizeListener) }
    addResizeListener(resizeListener)
    // Suspend until cancelled.
    suspendCancellableCoroutine<Nothing> { }
  }
