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
import android.content.Context.VIBRATOR_SERVICE
import android.os.Vibrator
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.LoadingBoardList
import com.squareup.sample.dungeon.GameSessionWorkflow.State.Loading
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineLayoutRunner
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.modal.AlertContainer
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource.Monotonic

private const val AI_COUNT = 4

/** Fake Dagger. */
@Suppress("MemberVisibilityCanBePrivate")
class Component(context: Context) {

  val viewRegistry = ViewRegistry(
      ShakeableTimeMachineLayoutRunner,
      LoadingBinding<LoadingBoardList>(R.string.loading_boards_list),
      BoardsListLayoutRunner,
      LoadingBinding<Loading>(R.string.loading_board),
      GameLayoutRunner,
      BoardView,
      AlertContainer
  )

  val random = Random(System.currentTimeMillis())

  @OptIn(ExperimentalTime::class)
  val clock = Monotonic

  val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator

  val boardLoader = BoardLoader(
      ioDispatcher = Dispatchers.IO,
      assets = context.assets,
      boardsAssetPath = "boards",
      delayForFakeLoad = context::delayForFakeLoad
  )

  val playerWorkflow = PlayerWorkflow()

  val aiWorkflows = List(AI_COUNT) { AiWorkflow(random = random) }

  val gameWorkflow = GameWorkflow(playerWorkflow, aiWorkflows, random)

  val gameSessionWorkflow = GameSessionWorkflow(gameWorkflow, vibrator, boardLoader)

  val appWorkflow = DungeonAppWorkflow(gameSessionWorkflow, boardLoader)

  val timeMachineWorkflow = TimeMachineAppWorkflow(appWorkflow, clock, context)
}
