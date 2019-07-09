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

import com.googlecode.lanterna.input.InputProvider
import com.googlecode.lanterna.input.KeyType.ArrowDown
import com.googlecode.lanterna.input.KeyType.ArrowLeft
import com.googlecode.lanterna.input.KeyType.ArrowRight
import com.googlecode.lanterna.input.KeyType.ArrowUp
import com.googlecode.lanterna.input.KeyType.Backspace
import com.googlecode.lanterna.input.KeyType.Character
import com.googlecode.lanterna.input.KeyType.EOF
import com.googlecode.lanterna.input.KeyType.Enter
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Unknown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.googlecode.lanterna.input.KeyStroke as LanternaKeystroke

/**
 * Represents a keyboard key being pressed.
 *
 * @param character The [Char] representing the key, or `null` if [keyType] is not [Character].
 * @param keyType The type of key that was pressed.
 */
data class KeyStroke(
  val character: Char?,
  val keyType: KeyType
) {
  enum class KeyType {
    Backspace,
    Character,
    ArrowUp,
    ArrowDown,
    ArrowLeft,
    ArrowRight,
    Enter,
    Unknown
  }
}

/**
 * Returns a new [Flow] of keystrokes from this input provider. The flow will _not_ be multicasted,
 * so you'll want to do that yourself or ensure that there is only ever one collector at a time.
 * You will also probably want to specify an IO dispatcher to run it on, since it does blocking
 * IO.
 *
 * This is a function instead of a property because calling it multiple times will return
 * independent flows.
 */
@Suppress("BlockingMethodInNonBlockingContext")
@UseExperimental(ExperimentalCoroutinesApi::class)
internal fun InputProvider.keyStrokes(): Flow<KeyStroke> = flow {
  coroutineScope {
    while (isActive) {
      // Since readInput doesn't take a timeout, we force it by interrupting the blocked thread
      // when we get cancelled.
      val keyStroke = interruptThreadOnCancel { readInput() }
      if (keyStroke.keyType === EOF) {
        // EOF indicates the terminal input was closed, and we won't receive any more input, so
        // close the channel instead of sending the raw event down.
        break
      }
      emit(keyStroke.toKeyStroke())
    }
  }
}

/**
 * Sends and interrupt to the calling thread when the [CoroutineScope] is cancelled. Intended to
 * make cancellation work correctly when [block] performs blocking IO – the blocked thread will not
 * get the cancellation signal, so we interrupt it to cancel the IO, then catch the interrupt
 * exception and throw the cancellation exception instead.
 */
@ExperimentalCoroutinesApi
private fun <T> CoroutineScope.interruptThreadOnCancel(block: () -> T): T {
  val thisThread = Thread.currentThread()

  // We can't just add an invokeOnCompletion handler to the current Job, because that won't get
  // invoked until the Job enters the "completed" state, which will be blocked by the block.
  // There is also a variant of invokeOnCompletion that accepts a "onCancelling" parameter which
  // we can pass as true to get the same behavior, but it's marked internal coroutines API.
  val interruptJob = launch(start = UNDISPATCHED) {
    suspendCancellableCoroutine<Nothing> {
      it.invokeOnCancellation { cause ->
        if (cause != null) thisThread.interrupt()
      }
    }
  }

  return try {
    block()
  } finally {
    // If the block exited normally, stop waiting for cancellation – the next time we're called,
    // it might be on a different thread.
    interruptJob.cancel()

    // Clear the interrupted flag.
    Thread.interrupted()

    // If we entered the finally because of a failure or regular cancellation, this will just
    // re-throw the existing cancellation exception (no-op).
    // If the thread was interrupted, the exception will be an InterruptedException (or similar –
    // e.g. Lanterna swallows and throws as a RuntimeException), and we don't care about that, we
    // want to throw the CancellationException that caused the thread to be interrupted in the first
    // place.
    ensureActive()
  }
}

private fun LanternaKeystroke.toKeyStroke(): KeyStroke = KeyStroke(
    character = character,
    keyType = when (keyType) {
      Character -> KeyType.Character
      Backspace -> KeyType.Backspace
      ArrowUp -> KeyType.ArrowUp
      ArrowDown -> KeyType.ArrowDown
      ArrowLeft -> KeyType.ArrowLeft
      ArrowRight -> KeyType.ArrowRight
      Enter -> KeyType.Enter
      else -> Unknown
    }
)
