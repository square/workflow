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

import com.squareup.sample.dungeon.ActorWorkflow.ActorInput
import com.squareup.sample.dungeon.ActorWorkflow.ActorRendering
import com.squareup.sample.dungeon.AiWorkflow.State
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.board.BoardCell
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.onWorkerOutput
import kotlinx.coroutines.flow.collect
import java.util.UUID
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * Simple AI workflow that just moves around in squiggly spirals and tries to eat the player.
 */
class AiWorkflow(
  private val avatar: BoardCell = BoardCell("👻"),
  private val random: Random,
  private val cellsPerSecond: Float = random.nextFloat() * 3f + 4f // Between 4 and 7.
) : ActorWorkflow, StatefulWorkflow<ActorInput, State, Nothing, ActorRendering>() {

  data class State(
    val direction: Direction,
    val directionTicker: Worker<Unit>
  )

  override fun initialState(
    input: ActorInput,
    snapshot: Snapshot?
  ): State {
    val startingDirection = random.nextEnum(Direction::class)
    return State(startingDirection, input.ticks.createDirectionTicker(random))
  }

  override fun onInputChanged(
    old: ActorInput,
    new: ActorInput,
    state: State
  ): State = if (!old.ticks.doesSameWorkAs(new.ticks)) {
    state.copy(directionTicker = new.ticks.createDirectionTicker(random))
  } else state

  override fun render(
    input: ActorInput,
    state: State,
    context: RenderContext<State, Nothing>
  ): ActorRendering {
    context.onWorkerOutput(state.directionTicker) {
      // Rotate 90 degrees.
      val newDirection = when (state.direction) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
      }
      println("AI changing direction from ${state.direction} to $newDirection")
      return@onWorkerOutput WorkflowAction {
        this.state = state.copy(direction = newDirection)
        null
      }
    }

    return ActorRendering(avatar, Movement(state.direction, cellsPerSecond = cellsPerSecond))
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY
}

private fun <T : Enum<T>> Random.nextEnum(enumClass: KClass<T>): T {
  val values = enumClass.java.enumConstants
  return values[nextInt(values.size)]
}

/**
 * Scales the tick frequency by a random amount to make direction changes look more arbitrary.
 */
private fun Worker<Long>.createDirectionTicker(random: Random): Worker<Unit> =
  Worker.create(key = UUID.randomUUID().toString()) {
    run()
        .collect { tick ->
          if (tick % random.nextInt(2, 5) == 0L) {
            emit(Unit)
          }
        }
  }
