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

import com.squareup.sample.dungeon.AiWorkflow.Input
import com.squareup.sample.dungeon.Board.Location
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.GameWorkflow.Output
import com.squareup.sample.dungeon.GameWorkflow.Output.PlayerWasEaten
import com.squareup.sample.dungeon.GameWorkflow.Output.Vibrate
import com.squareup.sample.dungeon.GameWorkflow.State
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import kotlin.random.Random

class GameWorkflow(
  private val playerWorkflow: PlayerWorkflow,
  private val aiWorkflows: List<AiWorkflow>,
  private val random: Random
) : StatefulWorkflow<Unit, State, Output, GameRendering>() {

  /**
   * @param finishedSnapshot If non-null, the game is finished and this was the last rendering
   * before the game finished.
   */
  data class State(
    val game: Game,
    val finishedSnapshot: GameRendering? = null
  )

  sealed class Output {
    /**
     * Emitted by [GameWorkflow] if the controller should be vibrated.
     */
    object Vibrate : Output()

    object PlayerWasEaten : Output()
  }

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): State {
    val board = Board(
        width = 16,
        height = 16,
        cells = Board.EMPTY
    )
    return State(game = Game(
        board = board,
        playerLocation = random.nextEmptyLocation(board),
        aiActors = aiWorkflows.map { random.nextEmptyLocation(board) }
    ))
  }

  override fun render(
    input: Unit,
    state: State,
    context: RenderContext<State, Output>
  ): GameRendering {
    // If the game has already finished, just render the finished game.
    state.finishedSnapshot?.let { return it }

    val game = state.game
    // Save the rendering before we return it, so we can put it in the state if something happens
    // that causes the game to finish.
    lateinit var rendering: GameRendering

    val player = context.renderChild(playerWorkflow, game) { movement ->
      val (newLocation, collided) = game.playerLocation.move(movement, game.board)
      val newGame = game.copy(playerLocation = newLocation)
      val output = if (collided) Vibrate else null
      return@renderChild enterState(State(newGame), emittingOutput = output)
    }

    val renderedAis = aiWorkflows.zip(game.aiActors)
        .mapIndexed { index, (aiWorkflow, aiLocation) ->
          val aiInput = Input(game.board, aiLocation)
          val aiCell =
            context.renderChild(aiWorkflow, aiInput, key = index.toString()) { movement ->
              val (newLocation, _) = aiLocation.move(movement, game.board)
              val newGame = game.copy(aiActors = game.aiActors.replaceAt(index, newLocation))

              // Check if AI captured player.
              return@renderChild if (newGame.isPlayerEaten) {
                enterState(State(newGame, rendering.freeze()), emittingOutput = PlayerWasEaten)
              } else {
                enterState(State(newGame))
              }
            }
          return@mapIndexed aiLocation to aiCell
        }

    val renderedBoard = game.board.withOverlay(
        renderedAis.toMap() + (game.playerLocation to player.avatar)
    )

    return GameRendering(renderedBoard, player)
        .also { rendering = it }
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY
}

private fun Random.nextEmptyLocation(board: Board): Location =
  generateSequence { nextLocation(board.width, board.height) }
      .first { (x, y) -> board[x, y].isEmpty }

private fun Random.nextLocation(
  width: Int,
  height: Int
) = Location(nextInt(width), nextInt(height))

private data class MoveResult(
  val newLocation: Location,
  val collisionDetected: Boolean
)

private fun Location.move(
  movement: Movement,
  board: Board
): MoveResult {
  var collisionDetected = false
  var (x, y) = this
  if (LEFT in movement) x -= 1
  if (RIGHT in movement) x += 1
  // Don't let the player leave the board.
  x = x.coerceIn(0 until board.width)
  // Don't allow collisions with obstacles on the board.
  if (!board[x, y].isEmpty) {
    collisionDetected = true
    x = this.x
  }

  if (UP in movement) y -= 1
  if (DOWN in movement) y += 1
  y = y.coerceIn(0 until board.height)
  if (!board[x, y].isEmpty) {
    collisionDetected = true
    y = this.y
  }

  return MoveResult(Location(x, y), collisionDetected)
}

/**
 * Removes event handlers from the rendering.
 */
private fun GameRendering.freeze(): GameRendering = copy(player = player.copy(onEvent = null))

private fun <T> List<T>.replaceAt(
  index: Int,
  newValue: T
): List<T> = mapIndexed { i, t -> if (index == i) newValue else t }
