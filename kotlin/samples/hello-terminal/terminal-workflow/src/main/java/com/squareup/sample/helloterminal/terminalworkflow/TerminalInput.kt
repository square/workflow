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

import com.squareup.workflow.Worker

/**
 * The `InputT` value fed into a [TerminalWorkflow] that describes the state of the terminal device.
 *
 * @param size The dimensions of the terminal during this render pass.
 * @param keyStrokes A [Worker] that emits a [KeyStroke] every time the user hits a keyboard key.
 * If a child workflow needs access to key events, you can just pass this worker down directly, or
 * you can wrap it in a worker that filters and/or transforms the key events that the child will
 * see, e.g. if the parent wants to intercept all "Enter" keystrokes.
 */
data class TerminalInput(
  val size: TerminalSize,
  val keyStrokes: Worker<KeyStroke>
)
