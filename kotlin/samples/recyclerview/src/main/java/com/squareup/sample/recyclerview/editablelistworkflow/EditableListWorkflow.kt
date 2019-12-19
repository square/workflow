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
package com.squareup.sample.recyclerview.editablelistworkflow

import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.Props
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.Rendering
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.State
import com.squareup.sample.recyclerview.inputrows.InputRow
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action

object EditableListWorkflow : StatefulWorkflow<Props, State, Nothing, Rendering>() {

  data class Props(val rows: List<InputRow<*, *>> = emptyList())

  data class State(val rowValues: List<RowValue<*, *>>)

  data class Rendering(
    val rowValues: List<RowValue<*, *>>,
    val onValueChanged: (position: Int, newValue: Any?) -> Unit
  )

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = State(props.rows.map { RowValue(it) })

  override fun onPropsChanged(
    old: Props,
    new: Props,
    state: State
  ): State {
    if (old.rows != new.rows) {
      return state.copy(
          rowValues = new.rows.mapIndexed { index, newRow ->
            if (index in state.rowValues.indices && newRow == state.rowValues[index].row) {
              state.rowValues[index]
            } else {
              RowValue(newRow)
            }
          })
    }
    return state
  }

  override fun render(
    props: Props,
    state: State,
    context: RenderContext<State, Nothing>
  ): Rendering {
    return Rendering(
        rowValues = state.rowValues,
        onValueChanged = { position, newValue ->
          context.actionSink.send(valueChangedAction(position, newValue))
        }
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private fun valueChangedAction(
    position: Int,
    newValue: Any?
  ) = action {
    nextState = nextState.copy(rowValues = nextState.rowValues.mapIndexed { index, value ->
      if (index == position) value.withValue(newValue) else value
    })
  }
}
