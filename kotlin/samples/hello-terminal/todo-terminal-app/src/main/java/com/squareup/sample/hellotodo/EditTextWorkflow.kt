package com.squareup.sample.hellotodo

import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowLeft
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.ArrowRight
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Backspace
import com.squareup.sample.helloterminal.terminalworkflow.KeyStroke.KeyType.Character
import com.squareup.sample.helloterminal.terminalworkflow.TerminalProps
import com.squareup.sample.hellotodo.EditTextWorkflow.EditTextProps
import com.squareup.sample.hellotodo.EditTextWorkflow.EditTextState
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action

class EditTextWorkflow : StatefulWorkflow<EditTextProps, EditTextState, String, String>() {

  data class EditTextProps(
    val text: String,
    val terminalProps: TerminalProps
  )

  /**
   * @param cursorPosition The index before which to draw the cursor (so 0 means before the first
   * character, `length` means after the last character).
   */
  data class EditTextState(
    val cursorPosition: Int
  )

  override fun initialState(
    props: EditTextProps,
    snapshot: Snapshot?
  ) = EditTextState(props.text.length)

  override fun onPropsChanged(
    old: EditTextProps,
    new: EditTextProps,
    state: EditTextState
  ): EditTextState {
    return if (old.text != new.text) {
      // Clamp the cursor position to the text length.
      state.copy(cursorPosition = state.cursorPosition.coerceIn(0..new.text.length))
    } else state
  }

  override fun render(
    props: EditTextProps,
    state: EditTextState,
    context: RenderContext<EditTextState, String>
  ): String {
    context.runningWorker(props.terminalProps.keyStrokes) { key -> onKeystroke(props, key) }

    return buildString {
      props.text.forEachIndexed { index, c ->
        append(if (index == state.cursorPosition) "|$c" else "$c")
      }
      if (state.cursorPosition == props.text.length) append("|")
    }
  }

  override fun snapshotState(state: EditTextState): Snapshot = Snapshot.EMPTY

  private fun onKeystroke(
    props: EditTextProps,
    key: KeyStroke
  ) = action {
    when (key.keyType) {
      Character -> {
        nextState = moveCursor(props, nextState, 1)
        props.text.insertCharAt(nextState.cursorPosition, key.character!!)
      }

      Backspace -> {
        if (props.text.isNotEmpty()) {
          nextState = moveCursor(props, nextState, -1)
          props.text.removeRange(nextState.cursorPosition - 1, nextState.cursorPosition)
        }
      }
      ArrowLeft -> nextState = moveCursor(props, nextState, -1)
      ArrowRight -> nextState = moveCursor(props, nextState, 1)
      else -> {
        // Nothing to do.
      }
    }
  }
}

private fun moveCursor(
  props: EditTextProps,
  state: EditTextState,
  delta: Int
): EditTextState =
  state.copy(cursorPosition = (state.cursorPosition + delta).coerceIn(0..props.text.length + 1))

private fun String.insertCharAt(
  index: Int,
  char: Char
): String = substring(0, index) + char + substring(index, length)
