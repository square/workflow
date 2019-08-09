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
