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
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowDown
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowUp
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Enter
import com.squareup.sample.helloterminal.terminalworkflow.TerminalProps
import com.squareup.sample.helloterminal.terminalworkflow.TerminalRendering
import com.squareup.sample.helloterminal.terminalworkflow.TerminalWorkflow
import com.squareup.sample.hellotodo.EditTextWorkflow.EditTextProps
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList
import com.squareup.sample.hellotodo.TodoWorkflow.TodoList.Companion.TITLE_FIELD_INDEX
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.runningWorker

private typealias TodoAction = WorkflowAction<TodoList, Nothing>

class TodoWorkflow : TerminalWorkflow,
    StatefulWorkflow<TerminalProps, TodoList, ExitCode, TerminalRendering>() {

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
    props: TerminalProps,
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
    props: TerminalProps,
    state: TodoList,
    context: RenderContext<TodoList, ExitCode>
  ): TerminalRendering {

    context.runningWorker(props.keyStrokes) { onKeystroke(it) }

    return TerminalRendering(buildString {
      appendln(state.renderTitle(props, context))
      appendln(renderSelection(state.titleSeparator, false))
      appendln(state.renderItems(props, context))
    })
  }

  override fun snapshotState(state: TodoList): Snapshot = Snapshot.EMPTY
}

private fun onKeystroke(key: KeyStroke): TodoAction = WorkflowAction {
  @Suppress("NON_EXHAUSTIVE_WHEN")
  when (key.keyType) {
    ArrowUp -> state = state.moveFocusUp()
    ArrowDown -> state = state.moveFocusDown()
    Enter -> if (state.focusedField > TITLE_FIELD_INDEX) {
      state = state.toggleChecked(state.focusedField)
    }
  }

  null
}

private fun updateTitle(newTitle: String): TodoAction = WorkflowAction {
  state = state.copy(title = newTitle)
  null
}

private fun setLabel(
  index: Int,
  text: String
): TodoAction = WorkflowAction {
  state = state.copy(items = state.items.mapIndexed { i, item ->
    if (index == i) item.copy(label = text) else item
  })
  null
}

private fun TodoList.renderTitle(
  props: TerminalProps,
  context: RenderContext<TodoList, *>
): String {
  val isSelected = focusedField == TITLE_FIELD_INDEX
  val titleString = if (isSelected) {
    context.renderChild(
        EditTextWorkflow(),
        props = EditTextProps(title, props),
        key = TITLE_FIELD_INDEX.toString()
    ) { updateTitle(it) }
  } else {
    title
  }
  return renderSelection(titleString, isSelected)
}

private val TodoList.titleSeparator get() = "–".repeat(title.length + 1)

private fun TodoList.renderItems(
  props: TerminalProps,
  context: RenderContext<TodoList, *>
): String =
  items
      .mapIndexed { index, item ->
        val check = if (item.checked) '✔' else ' '
        val isSelected = index == focusedField
        val label = if (isSelected) {
          context.renderChild(
              EditTextWorkflow(),
              props = EditTextProps(item.label, props),
              key = index.toString()
          ) { newText -> setLabel(index, newText) }
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
