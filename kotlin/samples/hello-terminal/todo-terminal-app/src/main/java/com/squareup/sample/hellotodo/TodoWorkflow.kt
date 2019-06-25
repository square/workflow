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
import com.squareup.sample.hellotodo.EditTextWorkflow.EditTextInput
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList.Companion.TITLE_FIELD_INDEX
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noAction
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

    fun setItemLabel(
      index: Int,
      newLabel: String
    ) = copy(items = items.mapIndexed { i, item ->
      if (index == i) item.copy(label = newLabel) else item
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
            noAction()
          }
        }
        else -> noAction()
      }
    }

    return TerminalRendering(buildString {
      appendln(state.renderTitle(input, context))
      appendln(renderSelection(state.titleSeparator, false))
      appendln(state.renderItems(input, context))
    })
  }

  override fun snapshotState(state: TodoList): Snapshot = Snapshot.EMPTY
}

private fun TodoList.renderTitle(
  input: TerminalInput,
  context: RenderContext<TodoList, *>
): String {
  val isSelected = focusedField == TITLE_FIELD_INDEX
  val titleString = if (isSelected) {
    context.renderChild(
        EditTextWorkflow(),
        input = EditTextInput(title, input),
        key = TITLE_FIELD_INDEX.toString()
    ) { newText -> enterState(copy(title = newText)) }
  } else {
    title
  }
  return renderSelection(titleString, isSelected)
}

private val TodoList.titleSeparator get() = "–".repeat(title.length + 1)

private fun TodoList.renderItems(
  input: TerminalInput,
  context: RenderContext<TodoList, *>
): String =
  items
      .mapIndexed { index, item ->
        val check = if (item.checked) '✔' else ' '
        val isSelected = index == focusedField
        val label = if (isSelected) {
          context.renderChild(
              EditTextWorkflow(),
              input = EditTextInput(item.label, input),
              key = index.toString()
          ) { newText -> enterState(setItemLabel(index, newText)) }
        } else {
          item.label
        }
        renderSelection("[$check] $label", isSelected)
      }
      .joinToString(separator = "\n")

private fun renderSelection(
  text: String,
  selected: Boolean
): String {
  val prefix = if (selected) "> " else "  "
  return prefix + text
}
