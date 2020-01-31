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
package com.squareup.sample.recyclerview

import com.squareup.sample.recyclerview.AppWorkflow.Action.CommitNewRowAction
import com.squareup.sample.recyclerview.AppWorkflow.Action.ShowAddRowAction
import com.squareup.sample.recyclerview.AppWorkflow.Rendering
import com.squareup.sample.recyclerview.AppWorkflow.State
import com.squareup.sample.recyclerview.AppWorkflow.State.ChooseNewRow
import com.squareup.sample.recyclerview.AppWorkflow.State.ShowList
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow
import com.squareup.sample.recyclerview.editablelistworkflow.EditableListWorkflow.Props
import com.squareup.sample.recyclerview.inputrows.CheckInputRow
import com.squareup.sample.recyclerview.inputrows.DropdownInputRow
import com.squareup.sample.recyclerview.inputrows.InputRow
import com.squareup.sample.recyclerview.inputrows.SwitchInputRow
import com.squareup.sample.recyclerview.inputrows.TextInputRow
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.renderChild
import com.squareup.workflow.ui.modal.HasModals

private val allRowTypes = listOf(
    TextInputRow,
    CheckInputRow,
    SwitchInputRow,
    DropdownInputRow
)

/**
 * Runs the [EditableListWorkflow] along with a button that lets you add new rows by popping up
 * a dialog.
 */
object AppWorkflow : StatefulWorkflow<Unit, State, Nothing, Rendering>() {

  sealed class State {
    abstract val rows: List<InputRow<*, *>>

    data class ShowList(override val rows: List<InputRow<*, *>>) : State()
    data class ChooseNewRow(override val rows: List<InputRow<*, *>>) : State()
  }

  data class BaseScreen(
    val listRendering: EditableListWorkflow.Rendering,
    val onAddRowTapped: () -> Unit
  )

  data class ChooseRowTypeScreen(
    val options: List<String>,
    val onSelectionTapped: (Int) -> Unit
  )

  data class Rendering(
    override val beneathModals: BaseScreen,
    val popup: ChooseRowTypeScreen? = null
  ) : HasModals<BaseScreen, ChooseRowTypeScreen> {
    override val modals: List<ChooseRowTypeScreen>
      get() = listOfNotNull(popup)
  }

  private sealed class Action : WorkflowAction<State, Nothing> {
    object ShowAddRowAction : Action()
    data class CommitNewRowAction(val index: Int) : Action()

    override fun Updater<State, Nothing>.apply() {
      nextState = when (this@Action) {
        ShowAddRowAction -> ChooseNewRow(nextState.rows)
        is CommitNewRowAction -> ShowList(nextState.rows + allRowTypes[index])
      }
    }
  }

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = ShowList(allRowTypes)

  override fun render(
    props: Unit,
    state: State,
    context: RenderContext<State, Nothing>
  ): Rendering {
    val listRendering = context.renderChild(EditableListWorkflow, Props(state.rows))
    val baseScreen = BaseScreen(
        listRendering = listRendering,
        onAddRowTapped = { context.actionSink.send(ShowAddRowAction) }
    )

    return when (state) {
      is ShowList -> Rendering(baseScreen)
      is ChooseNewRow -> Rendering(
          beneathModals = baseScreen,
          popup = ChooseRowTypeScreen(
              options = allRowTypes.map { it.description },
              onSelectionTapped = { index -> context.actionSink.send(CommitNewRowAction(index)) }
          )
      )
    }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY
}
