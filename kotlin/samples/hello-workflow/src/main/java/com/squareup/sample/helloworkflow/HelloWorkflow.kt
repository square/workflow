package com.squareup.sample.helloworkflow

import com.squareup.sample.helloworkflow.HelloWorkflow.Rendering
import com.squareup.sample.helloworkflow.HelloWorkflow.State
import com.squareup.sample.helloworkflow.HelloWorkflow.State.Goodbye
import com.squareup.sample.helloworkflow.HelloWorkflow.State.Hello
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion
import com.squareup.workflow.parse

object HelloWorkflow : StatefulWorkflow<Unit, State, Unit, Rendering>() {
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
    val onClick: (Unit) -> Unit
  )

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): State {
    return snapshot?.bytes?.parse { source ->
      if (source.readInt() == 1) Hello else Goodbye
    } ?: Hello
  }

  override fun render(
    input: Unit,
    state: State,
    context: RenderContext<State, Unit>
  ): Rendering {
    return Rendering(
        message = state.name,
        onClick = context.onEvent { Companion.enterState(state.theOtherState()) }
    )
  }

  override fun snapshotState(state: State): Snapshot {
    return Snapshot.of(if (state == Hello) 1 else 0)
  }
}
