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
import com.squareup.sample.dungeon.board.Board
import com.squareup.sample.dungeon.board.Board.Location
import com.squareup.sample.dungeon.board.BoardCell
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow

/**
 * Schema for a workflow that can plug into the [GameWorkflow] to represent an "actor" in the game.
 */
interface ActorWorkflow : Workflow<ActorProps, Nothing, ActorRendering> {

  data class ActorProps(
    val board: Board,
    val myLocation: Location,
    val ticks: Worker<Long>
  )

  data class ActorRendering(
    val avatar: BoardCell,
    val movement: Movement
  )
}
