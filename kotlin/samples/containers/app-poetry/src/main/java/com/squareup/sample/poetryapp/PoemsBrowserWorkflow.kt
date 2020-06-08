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
package com.squareup.sample.poetryapp

import com.squareup.sample.container.overviewdetail.OverviewDetailScreen
import com.squareup.sample.poetry.PoemWorkflow
import com.squareup.sample.poetry.model.Poem
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action
import com.squareup.workflow.parse
import com.squareup.workflow.ui.backstack.BackStackScreen

typealias SelectedPoem = Int

object PoemsBrowserWorkflow :
    StatefulWorkflow<List<Poem>, SelectedPoem, Nothing, OverviewDetailScreen>() {
  override fun initialState(
    props: List<Poem>,
    snapshot: Snapshot?
  ): SelectedPoem {
    return snapshot?.bytes?.parse { source -> source.readInt() }
        ?: -1
  }

  override fun render(
    props: List<Poem>,
    state: SelectedPoem,
    context: RenderContext<SelectedPoem, Nothing>
  ): OverviewDetailScreen {
    val poems: OverviewDetailScreen =
      context.renderChild(PoemListWorkflow, props) { selected -> choosePoem(selected) }
          .copy(selection = state)
          .let { OverviewDetailScreen(BackStackScreen(it)) }

    return if (state == -1) {
      poems
    } else {
      val poem: OverviewDetailScreen =
        context.renderChild(PoemWorkflow, props[state]) { clearSelection }
      poems + poem
    }
  }

  override fun snapshotState(state: SelectedPoem): Snapshot = Snapshot.write { sink ->
    sink.writeInt(state)
  }

  private fun choosePoem(index: SelectedPoem) = action("goToPoem") {
    nextState = index
  }

  private val clearSelection = choosePoem(-1)
}
