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

import com.squareup.sample.helloterminal.HelloTerminalWorkflow.State
import com.squareup.sample.helloterminal.terminalworkflow.ExitCode
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Backspace
import com.squareup.sample.helloterminal.terminalworkflow.TerminalProps
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering.Color.GREEN
import com.squareup.sample.helloterminal.terminalworkflow.TerminalWorkflow
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.action
import com.squareup.workflow.renderChild

private typealias HelloTerminalAction = WorkflowAction<State, ExitCode>

class HelloTerminalWorkflow : TerminalWorkflow,
    StatefulWorkflow<TerminalProps, State, ExitCode, TerminalRendering>() {

  data class State(
    val text: String = ""
  ) {
    fun backspace() = copy(text = text.dropLast(1))
    fun append(char: Char) = copy(text = text + char)
  }

  private val cursorWorkflow = BlinkingCursorWorkflow('_', 500)

  override fun initialState(
    props: TerminalProps,
    snapshot: Snapshot?
  ) = State()

  override fun render(
    props: TerminalProps,
    state: State,
    context: RenderContext<State, ExitCode>
  ): TerminalRendering {
    val (rows, columns) = props.size
    val header = """
          Hello world!

          Terminal dimensions: $rows rows ⨉ $columns columns


      """.trimIndent()

    val prompt = "> "
    val cursor = context.renderChild(cursorWorkflow)

    context.runningWorker(props.keyStrokes) { onKeystroke(it) }

    return TerminalRendering(
        text = header + prompt + state.text + cursor,
        textColor = GREEN
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private fun onKeystroke(key: KeyStroke): HelloTerminalAction = action {
    when {
      key.character == 'Q' -> setOutput(0)
      key.keyType == Backspace -> nextState = nextState.backspace()
      key.character != null -> nextState = nextState.append(key.character!!)
    }
  }
}
