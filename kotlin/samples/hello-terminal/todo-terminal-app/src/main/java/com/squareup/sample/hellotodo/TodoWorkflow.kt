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
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowDown
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowUp
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Enter
import com.squareup.sample.helloterminal.terminalworkflow.TerminalInput
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering
import com.squareup.sample.helloterminal.terminalworkflow.TerminalWorkflow
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList.Companion.TITLE_FIELD_INDEX
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.onWorkerOutput

class TodoWorkflow : TerminalWorkflow,
    StatefulWorkflow<TerminalInput, TodoList, ExitCode, TerminalRendering>() {

  data class TodoList(
    val title: String = "[untitled]",
    val items: List<TodoItem> = emptyList(),
    val focusedField: Int = TITLE_FIELD_INDEX
  ) {

    fun moveFocusUp() = copy(focusedField = (focusedField - 1).coerceAtLeast(TITLE_FIELD_INDEX))
    fun moveFocusDown() = copy(focusedField = (focusedField + 1).coerceAtMost(items.size - 1))
    fun toggleChecked(index: Int) = copy(items = items.mapIndexed { i, item ->
      item.copy(checked = item.checked xor (index == i))
    })

    companion object {
      const val TITLE_FIELD_INDEX = -1
    }
  }

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

    context.onWorkerOutput(input.keyStrokes) { key ->
      when (key.keyType) {
        ArrowUp -> enterState(state.moveFocusUp())
        ArrowDown -> enterState(state.moveFocusDown())
        Enter -> {
          if (state.focusedField > TITLE_FIELD_INDEX) {
            enterState(state.toggleChecked(state.focusedField))
          } else {
            noop()
          }
        }
        else -> noop()
      }
    }

    return TerminalRendering(buildString {
      appendln(renderSelection(state.title, state.focusedField == TITLE_FIELD_INDEX))
      appendln(renderSelection(state.titleSeparator, false))
      appendln(state.renderItems())
    })
  }

  override fun snapshotState(state: TodoList): Snapshot = Snapshot.EMPTY
}

private val TodoList.titleSeparator get() = "–".repeat(title.length)

private fun TodoList.renderItems(): String =
  items
      .mapIndexed { index, item ->
        val check = if (item.checked) '✔' else ' '
        renderSelection("[$check] ${item.label}", index == focusedField)
      }
      .joinToString(separator = "\n")

private fun renderSelection(
  text: String,
  selected: Boolean
): String {
  val prefix = if (selected) "> " else "  "
  return prefix + text
}
