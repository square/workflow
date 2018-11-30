package com.squareup.sample.tictactoe

import com.squareup.sample.tictactoe.RunGameState.MaybeQuitting
import com.squareup.sample.tictactoe.RunGameState.Playing
import com.squareup.viewbuilder.EventHandlingScreen.Companion.ignoreEvents
import com.squareup.viewbuilder.MainAndModalScreen
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool

object RunGameRenderer : Renderer<RunGameState, RunGameEvent, MainAndModalScreen<*, *>> {

  override fun render(
    state: RunGameState,
    workflow: WorkflowInput<RunGameEvent>,
    workflows: WorkflowPool
  ): MainAndModalScreen<*, *> {
    return when (state) {

      is Playing -> {
        return TakeTurnsRenderer
            .render(state.delegateState, workflows.input(state.id), workflows)
            .let { MainAndModalScreen<Any, Any>(it) }
      }

      is RunGameState.NewGame -> MainAndModalScreen<Any, Any>(NewGameScreen(workflow::sendEvent))

      is MaybeQuitting -> MainAndModalScreen(
          GamePlayScreen(state.completedGame.lastTurn, ignoreEvents()),
          ConfirmQuitScreen(workflow::sendEvent)
      )

      is RunGameState.GameOver -> MainAndModalScreen<Any, Any>(
          GameOverScreen(state, workflow::sendEvent)
      )
    }
  }
}
