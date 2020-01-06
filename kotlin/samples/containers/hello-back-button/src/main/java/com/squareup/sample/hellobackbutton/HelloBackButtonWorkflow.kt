/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.hellobackbutton

import com.squareup.sample.hellobackbutton.HelloBackButtonWorkflow.Rendering
import com.squareup.sample.hellobackbutton.HelloBackButtonWorkflow.State
import com.squareup.sample.hellobackbutton.HelloBackButtonWorkflow.State.Able
import com.squareup.sample.hellobackbutton.HelloBackButtonWorkflow.State.Baker
import com.squareup.sample.hellobackbutton.HelloBackButtonWorkflow.State.Charlie
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action

object HelloBackButtonWorkflow : StatefulWorkflow<Unit, State, Nothing, Rendering>() {
  enum class State {
    Able,
    Baker,
    Charlie;
  }

  data class Rendering(
    val message: String,
    val onClick: () -> Unit,
    val onBackPressed: (() -> Unit)?
  )

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = Able

  override fun render(
    props: Unit,
    state: State,
    context: RenderContext<State, Nothing>
  ): Rendering {
    return Rendering(
        message = "$state",
        onClick = { context.actionSink.send(advance) },
        onBackPressed = { context.actionSink.send(retreat) }.takeIf { state != Able }
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  private val advance = action {
    nextState = when (nextState) {
      Able -> Baker
      Baker -> Charlie
      Charlie -> Able
    }
  }

  private val retreat = action {
    nextState = when (nextState) {
      Able -> throw IllegalStateException()
      Baker -> Able
      Charlie -> Baker
    }
  }
}
