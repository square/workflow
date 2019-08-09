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
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.GameWorkflow.GameRendering
import com.squareup.sample.dungeon.GameWorkflow.Output
import com.squareup.sample.dungeon.GameWorkflow.Output.PlayerWasEaten
import com.squareup.sample.dungeon.GameWorkflow.Output.Vibrate
import com.squareup.sample.dungeon.GameWorkflow.State
import com.squareup.sample.dungeon.board.Board
import com.squareup.sample.dungeon.board.Board.Location
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.onWorkerOutput
import com.squareup.workflow.renderChild
import kotlin.math.roundToLong
import kotlin.random.Random

class GameWorkflow(
  private val playerWorkflow: PlayerWorkflow,
  private val aiWorkflows: List<ActorWorkflow>,
  private val ticker: GameTicker,
  private val random: Random
) : StatefulWorkflow<Board, State, Output, GameRendering>() {

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

  data class GameRendering(
    val board: Board,
    val onPlayerEvent: ((PlayerWorkflow.Event) -> Unit)?
  )

  override fun initialState(
    input: Board,
    snapshot: Snapshot?
  ): State {
    return State(game = Game(
        board = input,
        playerLocation = random.nextEmptyLocation(input),
        aiLocations = aiWorkflows.map { random.nextEmptyLocation(input) }
    ))
  }

  override fun render(
    input: Board,
    state: State,
    context: RenderContext<State, Output>
  ): GameRendering {
    // If the game has already finished, just render the finished game.
    state.finishedSnapshot?.let { return it }

    val game = state.game
    val board = game.board
    // Save the rendering before we return it, so we can put it in the state if something happens
    // that causes the game to finish.
    lateinit var rendering: GameRendering

    // Render the player.
    val playerInput = ActorInput(board, game.playerLocation, ticker.ticks)
    val playerRendering = context.renderChild(playerWorkflow, playerInput)

    // Render all the other actors.
    val aiRenderings = aiWorkflows.zip(game.aiLocations)
        .mapIndexed { index, (aiWorkflow, aiLocation) ->
          val aiInput = ActorInput(game.board, aiLocation, ticker.ticks)
          aiLocation to context.renderChild(aiWorkflow, aiInput, key = index.toString())
        }

    // Calculate new locations for player and other actors.
    context.onWorkerOutput(ticker.ticks) { tick ->
      // Calculate if this tick should result in movement based on the movement's speed.
      fun Movement.isTimeToMove(): Boolean {
        val ticksPerSecond = ticker.ticksPerSecond
        val ticksPerCell = (ticksPerSecond / cellsPerSecond).roundToLong()
        return tick % ticksPerCell == 0L
      }

      // Execute player movement.
      var output: Output? = null
      var newPlayerLocation: Location = game.playerLocation
      if (playerRendering.actorRendering.movement.isTimeToMove()) {
        val moveResult = game.playerLocation.move(playerRendering.actorRendering.movement, board)
        newPlayerLocation = moveResult.newLocation
        if (moveResult.collisionDetected) output = Vibrate
      }

      // Execute AI movement.
      val newAiLocations = aiRenderings.map { (location, rendering) ->
        return@map if (rendering.movement.isTimeToMove()) {
          location.move(rendering.movement, board)
              // Don't care about collisions.
              .newLocation
        } else {
          location
        }
      }

      val newGame = game.copy(
          playerLocation = newPlayerLocation,
          aiLocations = newAiLocations
      )

      // Check if AI captured player.
      return@onWorkerOutput if (newGame.isPlayerEaten) {
        enterState(
            state.copy(game = newGame, finishedSnapshot = rendering.freeze()),
            emittingOutput = PlayerWasEaten
        )
      } else {
        enterState(
            state.copy(game = newGame),
            emittingOutput = output
        )
      }
    }

    val aiOverlay = aiRenderings.map { (a, b) -> a to b.avatar }
        .toMap()
    val renderedBoard = game.board.withOverlay(
        aiOverlay + (game.playerLocation to playerRendering.actorRendering.avatar)
    )
    return GameRendering(renderedBoard, playerRendering.onEvent)
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
private fun GameRendering.freeze(): GameRendering = copy(onPlayerEvent = null)
