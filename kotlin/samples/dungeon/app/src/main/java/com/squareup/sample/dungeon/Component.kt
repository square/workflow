package com.squareup.sample.dungeon

import android.content.Context
import android.os.Vibrator
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.ViewRegistry
import kotlin.random.Random

private const val AI_COUNT = 4

/** Fake Dagger. */
@Suppress("MemberVisibilityCanBePrivate")
@UseExperimental(ExperimentalWorkflowUi::class)
class Component(context: Context) {

  val viewRegistry = ViewRegistry(BoardView, GameLayoutRunner)

  val random = Random(System.currentTimeMillis())

  val vibrator = context.getSystemService(Vibrator::class.java)!!

  val ticker = GameTicker()

  val playerWorkflow = PlayerWorkflow(ticker = ticker)

  val aiWorkflows = List(AI_COUNT) { AiWorkflow(random = random, ticker = ticker) }

  val gameWorkflow = GameWorkflow(playerWorkflow, aiWorkflows, random)

  val appWorkflow = DungeonAppWorkflow(gameWorkflow, vibrator)
}
