package com.squareup.sample.helloterminal.terminalworkflow

import com.squareup.workflow.Worker

/**
 * The `PropsT` value fed into a [TerminalWorkflow] that describes the state of the terminal device.
 *
 * @param size The dimensions of the terminal during this render pass.
 * @param keyStrokes A [Worker] that emits a [KeyStroke] every time the user hits a keyboard key.
 * If a child workflow needs access to key events, you can just pass this worker down directly, or
 * you can wrap it in a worker that filters and/or transforms the key events that the child will
 * see, e.g. if the parent wants to intercept all "Enter" keystrokes.
 */
data class TerminalProps(
  val size: TerminalSize,
  val keyStrokes: Worker<KeyStroke>
)
