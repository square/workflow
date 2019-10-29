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
package com.squareup.sample.hellocompose

import com.squareup.sample.hellocompose.HelloWorkflow.Rendering
import com.squareup.sample.hellocompose.HelloWorkflow.State
import com.squareup.sample.hellocompose.HelloWorkflow.State.Goodbye
import com.squareup.sample.hellocompose.HelloWorkflow.State.Hello
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.parse

object HelloWorkflow : StatefulWorkflow<Unit, State, Nothing, Rendering>() {
  enum class State {
    Hello,
    Goodbye;

    fun theOtherState(): State = when (this) {
      Hello -> Goodbye
      Goodbye -> Hello
    }
  }

  data class Rendering(
    val message: String,
    val onClick: () -> Unit
  )

  private val helloAction = WorkflowAction<State, Nothing> {
    state = state.theOtherState()
    null
  }

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = snapshot?.bytes?.parse { source -> if (source.readInt() == 1) Hello else Goodbye }
      ?: Hello

  override fun render(
    props: Unit,
    state: State,
    context: RenderContext<State, Nothing>
  ): Rendering {
    val sink = context.makeActionSink<WorkflowAction<State, Nothing>>()
    return Rendering(
        message = state.name,
        onClick = { sink.send(helloAction) }
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.of(if (state == Hello) 1 else 0)
}
