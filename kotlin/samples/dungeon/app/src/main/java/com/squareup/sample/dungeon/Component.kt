package com.squareup.sample.dungeon

import android.content.Context
import android.os.Vibrator
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.ViewRegistry

/** Fake Dagger. */
@Suppress("MemberVisibilityCanBePrivate")
@UseExperimental(ExperimentalWorkflowUi::class)
class Component(context: Context) {

  val viewRegistry = ViewRegistry(BoardView, GameLayoutRunner)

  val vibrator = context.getSystemService(Vibrator::class.java)!!

  val ticker = GameTicker()

  val playerWorkflow = PlayerWorkflow(ticker)

  val gameWorkflow = GameWorkflow(playerWorkflow)

  val appWorkflow = DungeonAppWorkflow(gameWorkflow, vibrator)
}
