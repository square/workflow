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
package com.squareup.sample.hellotodo

import com.squareup.sample.helloterminal.terminalworkflow.ExitCode
import com.squareup.sample.helloterminal.terminalworkflow.TerminalInput
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering
import com.squareup.sample.helloterminal.terminalworkflow.TerminalWorkflow
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow

class TodoWorkflow : TerminalWorkflow,
    StatefulWorkflow<TerminalInput, TodoList, ExitCode, TerminalRendering>() {

  data class TodoList(
    val title: String = "[untitled]",
    val items: List<TodoItem> = emptyList()
  )

  data class TodoItem(
    val label: String,
    val checked: Boolean = false
  )

  override fun initialState(
    input: TerminalInput,
    snapshot: Snapshot?
  ) = TodoList(
      title = "Grocery list",
      items = listOf(
          TodoItem("eggs"),
          TodoItem("cheese"),
          TodoItem("bread"),
          TodoItem("beer")
      )
  )

  override fun render(
    input: TerminalInput,
    state: TodoList,
    context: RenderContext<TodoList, ExitCode>
  ): TerminalRendering {
    return TerminalRendering(buildString {
      appendln(state.title)
      appendln(state.titleSeparator)
      appendln(state.renderItems())
    })
  }

  override fun snapshotState(state: TodoList): Snapshot = Snapshot.EMPTY
}

private val TodoList.titleSeparator get() = "–".repeat(title.length)

private fun TodoList.renderItems(): String = items.joinToString(separator = "\n") {
  val check = if (it.checked) '✔' else ' '
  "[$check] ${it.label}"
}
