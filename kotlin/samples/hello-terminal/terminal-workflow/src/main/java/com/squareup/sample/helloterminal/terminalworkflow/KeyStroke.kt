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
import com.googlecode.lanterna.input.KeyType.Backspace
import com.googlecode.lanterna.input.KeyType.Character
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Unknown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.isActive
import com.googlecode.lanterna.input.KeyStroke as LanternaKeystroke

/**
 * TODO kdoc
 */
data class KeyStroke(
  val character: Char?,
  val keyType: KeyType,
  internal val source: LanternaKeystroke
) {
  enum class KeyType {
    Backspace,
    Character,
    Unknown
  }
}

internal fun LanternaKeystroke.toKeyStroke(): KeyStroke = KeyStroke(
    character = character,
    keyType = when (keyType) {
      Character -> KeyType.Character
      Backspace -> KeyType.Backspace
      else -> Unknown
    },
    source = this
)

@Suppress("BlockingMethodInNonBlockingContext")
@UseExperimental(ExperimentalCoroutinesApi::class)
internal fun InputProvider.listenForKeyStrokesOn(
  scope: CoroutineScope
): ReceiveChannel<LanternaKeystroke> = scope.produce {
  while (isActive) {
    val keyStroke = readInput()
    println("1: $keyStroke")
    send(keyStroke)
  }
}
