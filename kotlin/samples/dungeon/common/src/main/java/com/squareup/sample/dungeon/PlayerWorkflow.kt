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

import com.squareup.sample.dungeon.ActorWorkflow.ActorProps
import com.squareup.sample.dungeon.ActorWorkflow.ActorRendering
import com.squareup.sample.dungeon.PlayerWorkflow.Action.StartMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Action.StopMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Rendering
import com.squareup.sample.dungeon.board.BoardCell
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater

/**
 * Workflow that represents the actual player of the game in the [GameWorkflow].
 */
class PlayerWorkflow(
  private val avatar: BoardCell = BoardCell("👩🏻‍🎤"),
  private val cellsPerSecond: Float = 15f
) : StatefulWorkflow<ActorProps, Movement, Nothing, Rendering>() {

  sealed class Action : WorkflowAction<Movement, Nothing> {

    class StartMoving(private val direction: Direction) : Action() {
      override fun Updater<Movement, Nothing>.apply() {
        nextState += direction
      }
    }

    class StopMoving(private val direction: Direction) : Action() {
      override fun Updater<Movement, Nothing>.apply() {
        nextState -= direction
      }
    }
  }

  data class Rendering(
    val actorRendering: ActorRendering,
    val onStartMoving: (Direction) -> Unit,
    val onStopMoving: (Direction) -> Unit
  )

  override fun initialState(
    props: ActorProps,
    snapshot: Snapshot?
  ): Movement = Movement(cellsPerSecond = cellsPerSecond)

  override fun render(
    props: ActorProps,
    state: Movement,
    context: RenderContext<Movement, Nothing>
  ): Rendering = Rendering(
      actorRendering = ActorRendering(avatar = avatar, movement = state),
      onStartMoving = { context.actionSink.send(StartMoving(it)) },
      onStopMoving = { context.actionSink.send(StopMoving(it)) }
  )

  override fun snapshotState(state: Movement): Snapshot = Snapshot.EMPTY
}
