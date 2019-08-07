package com.squareup.sample.dungeon

import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.ViewRegistry

/** Fake Dagger. */
@UseExperimental(ExperimentalWorkflowUi::class)
object Component {

  val viewRegistry = ViewRegistry(BoardView, GameLayoutRunner)

  val ticker = GameTicker()

  val playerWorkflow = PlayerWorkflow(ticker)

  val gameWorkflow = GameWorkflow(playerWorkflow)

  val appWorkflow = DungeonAppWorkflow(gameWorkflow)
}
