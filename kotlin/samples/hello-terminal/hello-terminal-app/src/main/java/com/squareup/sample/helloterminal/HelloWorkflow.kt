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
package com.squareup.sample.helloterminal

import com.squareup.sample.helloterminal.HelloWorkflow.State
import com.squareup.sample.helloterminal.terminalworkflow.ExitCode
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Backspace
import com.squareup.sample.helloterminal.terminalworkflow.TerminalConfig
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.renderChild

class HelloWorkflow : StatefulWorkflow<TerminalConfig, State, ExitCode, TerminalRendering>() {

  data class State(
    val text: String = ""
  ) {
    fun backspace() = copy(text = text.dropLast(1))
    fun append(char: Char) = copy(text = text + char)
  }

  private val cursorWorkflow = BlinkingCursorWorkflow('_', 1000)

  override fun initialState(
    input: TerminalConfig,
    snapshot: Snapshot?
  ) = State()

  override fun render(
    input: TerminalConfig,
    state: State,
    context: RenderContext<State, ExitCode>
  ): TerminalRendering {
    val (rows, columns) = input.size
    val header = """
          Hello world!

          Terminal dimensions: $rows rows ⨉ $columns columns


      """.trimIndent()

    val prompt = "> "
    val cursor = context.renderChild(cursorWorkflow)

    return TerminalRendering(
        text = header + prompt + state.text + cursor,
        onKeyStroke = context.onEvent { key ->
          when {
            key.keyType == Backspace -> enterState(state.backspace())
            key.character == 'Q' -> emitOutput(0)
            key.character != null -> enterState(state.append(key.character!!))
            else -> noop()
          }
        }
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY
}
