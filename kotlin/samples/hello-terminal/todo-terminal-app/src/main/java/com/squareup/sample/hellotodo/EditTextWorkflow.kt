package com.squareup.sample.hellotodo

import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowLeft
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowRight
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Backspace
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Character
import com.squareup.sample.helloterminal.terminalworkflow.TerminalInput
import com.squareup.sample.hellotodo.EditTextWorkflow.EditTextInput
import com.squareup.sample.hellotodo.EditTextWorkflow.EditTextState
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.onWorkerOutput

class EditTextWorkflow : StatefulWorkflow<EditTextInput, EditTextState, String, String>() {

  data class EditTextInput(
    val text: String,
    val terminalInput: TerminalInput
  )

  /**
   * @param cursorPosition The index before which to draw the cursor (so 0 means before the first
   * character, `length` means after the last character).
   */
  data class EditTextState(
    val cursorPosition: Int
  )

  override fun initialState(
    input: EditTextInput,
    snapshot: Snapshot?
  ) = EditTextState(input.text.length)

  override fun onInputChanged(
    old: EditTextInput,
    new: EditTextInput,
    state: EditTextState
  ): EditTextState {
    return if (old.text != new.text) {
      // Clamp the cursor position to the text length.
      state.copy(cursorPosition = state.cursorPosition.coerceIn(0..new.text.length))
    } else state
  }

  override fun render(
    input: EditTextInput,
    state: EditTextState,
    context: RenderContext<EditTextState, String>
  ): String {
    context.onWorkerOutput(input.terminalInput.keyStrokes) { key ->
      when (key.keyType) {
        Character -> enterState(
            moveCursor(input, state, 1),
            emittingOutput = input.text.insertCharAt(state.cursorPosition, key.character!!)
        )
        Backspace -> {
          if (input.text.isEmpty()) {
            noop()
          } else {
            enterState(
                moveCursor(input, state, -1),
                emittingOutput = input.text.removeRange(
                    state.cursorPosition - 1, state.cursorPosition
                )
            )
          }
        }
        ArrowLeft -> enterState(moveCursor(input, state, -1))
        ArrowRight -> enterState(moveCursor(input, state, 1))
        else -> noop()
      }
    }

    return buildString {
      input.text.forEachIndexed { index, c ->
        append(if (index == state.cursorPosition) "|$c" else "$c")
      }
      if (state.cursorPosition == input.text.length) append("|")
    }
  }

  override fun snapshotState(state: EditTextState): Snapshot = Snapshot.EMPTY
}

private fun moveCursor(
  input: EditTextInput,
  state: EditTextState,
  delta: Int
): EditTextState =
  state.copy(cursorPosition = (state.cursorPosition + delta).coerceIn(0..input.text.length + 1))

private fun String.insertCharAt(
  index: Int,
  char: Char
): String = substring(0, index) + char + substring(index, length)
