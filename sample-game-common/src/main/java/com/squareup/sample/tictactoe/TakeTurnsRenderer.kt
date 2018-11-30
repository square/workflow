package com.squareup.sample.tictactoe

import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput

object TakeTurnsRenderer : Renderer<Turn, TakeTurnsEvent, GamePlayScreen> {
  override fun render(
    state: Turn,
    workflow: WorkflowInput<TakeTurnsEvent>,
    workflows: WorkflowPool
  ): GamePlayScreen = GamePlayScreen(state, workflow::sendEvent)
}
