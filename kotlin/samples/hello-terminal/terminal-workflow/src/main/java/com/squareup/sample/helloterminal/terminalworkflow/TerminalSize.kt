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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.suspendCancellableCoroutine
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

@OptIn(ExperimentalCoroutinesApi::class)
internal fun Terminal.listenForResizesOn(
  scope: CoroutineScope
): ReceiveChannel<TerminalSize> =
// Run with unconfined dispatcher because this is just sending events to a channel, we don't care
  // what thread it's on.
  scope.produce(context = Unconfined, capacity = Channel.CONFLATED) {
    val resizeListener = TerminalResizeListener { _, newSize -> offer(newSize.toSize()) }
    invokeOnClose { removeResizeListener(resizeListener) }
    addResizeListener(resizeListener)
    // Suspend until cancelled.
    suspendCancellableCoroutine<Nothing> { }
  }

internal fun LanternaTerminalSize.toSize(): TerminalSize = TerminalSize(rows, columns)
