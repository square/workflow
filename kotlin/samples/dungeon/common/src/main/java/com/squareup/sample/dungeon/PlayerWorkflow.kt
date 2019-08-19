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
import com.squareup.sample.dungeon.PlayerWorkflow.Action.StartMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Action.StopMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Rendering
import com.squareup.sample.dungeon.board.BoardCell
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Mutator

/**
 * Workflow that represents the actual player of the game in the [GameWorkflow].
 */
class PlayerWorkflow(
  private val avatar: BoardCell = BoardCell("👩🏻‍🎤"),
  private val cellsPerSecond: Float = 15f
) : StatefulWorkflow<ActorInput, Movement, Nothing, Rendering>() {

  sealed class Action : WorkflowAction<Movement, Nothing> {
    class StartMoving(private val direction: Direction) : Action() {
      override fun Mutator<Movement>.apply(): Nothing? {
        state += direction
        return null
      }
    }

    class StopMoving(private val direction: Direction) : Action() {
      override fun Mutator<Movement>.apply(): Nothing? {
        state -= direction
        return null
      }
    }
  }

  data class Rendering(
    val actorRendering: ActorRendering,
    val onStartMoving: (Direction) -> Unit,
    val onStopMoving: (Direction) -> Unit
  )

  override fun initialState(
    input: ActorInput,
    snapshot: Snapshot?
  ): Movement = Movement(cellsPerSecond = cellsPerSecond)

  override fun render(
    input: ActorInput,
    state: Movement,
    context: RenderContext<Movement, Nothing>
  ): Rendering {
    val sink = context.makeActionSink<Action>()

    return Rendering(
        actorRendering = ActorRendering(avatar = avatar, movement = state),
        onStartMoving = { sink.send(StartMoving(it)) },
        onStopMoving = { sink.send(StopMoving(it)) }
    )
  }

  override fun snapshotState(state: Movement): Snapshot = Snapshot.EMPTY
}
