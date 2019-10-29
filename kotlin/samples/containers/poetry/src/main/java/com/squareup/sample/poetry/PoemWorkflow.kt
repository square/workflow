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
package com.squareup.sample.poetry

import com.squareup.sample.container.masterdetail.MasterDetailScreen
import com.squareup.sample.poetry.StanzaWorkflow.Output.Exit
import com.squareup.sample.poetry.StanzaWorkflow.Output.GoBack
import com.squareup.sample.poetry.StanzaWorkflow.Output.GoForth
import com.squareup.sample.poetry.StanzaWorkflow.Props
import com.squareup.sample.poetry.StanzaWorkflow.Rendering
import com.squareup.sample.poetry.model.Poem
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.parse
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.workflowAction

object PoemWorkflow : StatefulWorkflow<Poem, Int, Nothing, MasterDetailScreen>() {
  override fun initialState(
    props: Poem,
    snapshot: Snapshot?
  ): Int {
    return snapshot?.bytes?.parse { source -> source.readInt() }
        ?: -1
  }

  override fun render(
    props: Poem,
    state: Int,
    context: RenderContext<Int, Nothing>
  ): MasterDetailScreen {
    val previousStanzas: List<Rendering> =
      if (state == -1) emptyList()
      else props.stanzas.subList(0, state)
          .mapIndexed { index, _ ->
            context.renderChild(StanzaWorkflow, Props(props, index), "$index") {
              noAction()
            }
          }

    val visibleStanza =
      if (state < 0) {
        null
      } else {
        context.renderChild(
            StanzaWorkflow, Props(props, state), "$state"
        ) {
          when (it) {
            Exit -> closePoem
            GoBack -> goBack
            GoForth -> goForth
          }
        }
      }

    val stackedStanzas = visibleStanza?.let {
      BackStackScreen<Any>(
          backStack = previousStanzas,
          top = visibleStanza
      )
    }

    val index =
      context.renderChild(StanzasWorkflow, props) { selected -> goTo(selected) }
          .copy(selection = state)

    val sink = context.makeActionSink<WorkflowAction<Int, Nothing>>()

    return MasterDetailScreen(
        masterRendering = BackStackScreen(index),
        detailRendering = stackedStanzas,
        selectDefault = { sink.send(goForth) }
    )
  }

  override fun snapshotState(state: Int): Snapshot = Snapshot.write { sink ->
    sink.writeInt(state)
  }

  private val closePoem: WorkflowAction<Int, Nothing> = workflowAction {
    state = -1
    null
  }

  private val goBack: WorkflowAction<Int, Nothing> = workflowAction {
    state -= 1
    null
  }

  private val goForth: WorkflowAction<Int, Nothing> = workflowAction {
    state += 1
    null
  }

  private fun goTo(index: Int): WorkflowAction<Int, Nothing> = workflowAction {
    state = index
    null
  }
}
