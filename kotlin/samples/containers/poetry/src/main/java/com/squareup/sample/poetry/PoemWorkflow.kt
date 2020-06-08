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
@file:Suppress("DEPRECATION", "OverridingDeprecatedMember")

package com.squareup.sample.poetry

import com.squareup.sample.container.overviewdetail.OverviewDetailScreen
import com.squareup.sample.poetry.PoemWorkflow.Action.ClearSelection
import com.squareup.sample.poetry.PoemWorkflow.Action.HandleStanzaListOutput
import com.squareup.sample.poetry.PoemWorkflow.Action.SelectNext
import com.squareup.sample.poetry.PoemWorkflow.Action.SelectPrevious
import com.squareup.sample.poetry.PoemWorkflow.ClosePoem
import com.squareup.sample.poetry.StanzaWorkflow.Output.CloseStanzas
import com.squareup.sample.poetry.StanzaWorkflow.Output.ShowNextStanza
import com.squareup.sample.poetry.StanzaWorkflow.Output.ShowPreviousStanza
import com.squareup.sample.poetry.StanzaWorkflow.Props
import com.squareup.sample.poetry.model.Poem
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.WorkflowAction.Mutator
import com.squareup.workflow.parse
import com.squareup.workflow.ui.backstack.BackStackScreen
import com.squareup.workflow.ui.backstack.toBackStackScreen

/**
 * Renders a [Poem] as a [OverviewDetailScreen], whose overview is a [StanzaListRendering]
 * for the poem, and whose detail traverses through [StanzaRendering]s.
 */
object PoemWorkflow : StatefulWorkflow<Poem, Int, ClosePoem, OverviewDetailScreen>() {
  object ClosePoem

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
    context: RenderContext<Int, ClosePoem>
  ): OverviewDetailScreen {
    val previousStanzas: List<StanzaRendering> =
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
            CloseStanzas -> ClearSelection
            ShowPreviousStanza -> SelectPrevious
            ShowNextStanza -> SelectNext
          }
        }
      }

    val stackedStanzas = visibleStanza?.let {
      (previousStanzas + visibleStanza).toBackStackScreen<Any>()
    }

    val stanzaIndex =
      context.renderChild(StanzaListWorkflow, props) { selected ->
        HandleStanzaListOutput(selected)
      }
          .copy(selection = state)
          .let { BackStackScreen<Any>(it) }

    return stackedStanzas
        ?.let { OverviewDetailScreen(overviewRendering = stanzaIndex, detailRendering = it) }
        ?: OverviewDetailScreen(
            overviewRendering = stanzaIndex,
            selectDefault = { context.actionSink.send(HandleStanzaListOutput(0)) }
        )
  }

  override fun snapshotState(state: Int): Snapshot = Snapshot.write { sink ->
    sink.writeInt(state)
  }

  private sealed class Action : WorkflowAction<Int, ClosePoem> {
    object ClearSelection : Action()
    object SelectPrevious : Action()
    object SelectNext : Action()
    class HandleStanzaListOutput(val selection: Int) : Action()
    object ExitPoem : Action()

    // We continue to use the deprecated method here for one more release, to demonstrate
    // that the migration mechanism works.
    override fun Mutator<Int>.apply(): ClosePoem? {
      when (this@Action) {
        ClearSelection -> state = -1
        SelectPrevious -> state -= 1
        SelectNext -> state += 1
        is HandleStanzaListOutput -> {
          if (selection == -1) return ClosePoem
          state = selection
        }
        ExitPoem -> return ClosePoem
      }

      return null
    }
  }
}
