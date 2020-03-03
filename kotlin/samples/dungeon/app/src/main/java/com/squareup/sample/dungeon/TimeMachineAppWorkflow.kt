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
import com.squareup.sample.dungeon.DungeonAppWorkflow.Props
import com.squareup.sample.timemachine.TimeMachineWorkflow
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineRendering
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineWorkflow
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineWorkflow.PropsFactory
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.renderChild
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * A workflow that wraps [DungeonAppWorkflow] with a [ShakeableTimeMachineWorkflow] to enable
 * time travel debugging.
 */
@OptIn(ExperimentalTime::class)
class TimeMachineAppWorkflow(
  appWorkflow: DungeonAppWorkflow,
  clock: TimeSource,
  context: Context
) : StatelessWorkflow<BoardPath, Nothing, ShakeableTimeMachineRendering>() {

  private val timeMachineWorkflow =
    ShakeableTimeMachineWorkflow(TimeMachineWorkflow(appWorkflow, clock), context)

  override fun render(
    props: BoardPath,
    context: RenderContext<Nothing, Nothing>
  ): ShakeableTimeMachineRendering {
    val propsFactory = PropsFactory { recording ->
      Props(paused = !recording)
    }
    return context.renderChild(timeMachineWorkflow, propsFactory)
  }
}
