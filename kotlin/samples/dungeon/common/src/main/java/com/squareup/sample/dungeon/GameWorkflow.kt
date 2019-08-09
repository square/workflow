package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.AiWorkflow.Input
import com.squareup.sample.dungeon.Board.Location
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.GameWorkflow.Output
import com.squareup.sample.dungeon.GameWorkflow.Output.Vibrate
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import kotlin.random.Random

class GameWorkflow(
  private val playerWorkflow: PlayerWorkflow,
  private val aiWorkflows: List<AiWorkflow>,
  private val random: Random
) : StatefulWorkflow<Unit, Game, Output, GameRendering>() {

  sealed class Output {
    /**
     * Emitted by [GameWorkflow] if the controller should be vibrated.
     */
    object Vibrate : Output()
  }

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): Game {
    val board = Board(
        width = 16,
        height = 16,
        cells = Board.EMPTY
    )
    return Game(
        board = board,
        playerLocation = random.nextEmptyLocation(board),
        aiActors = aiWorkflows.map { random.nextEmptyLocation(board) }
    )
  }

  override fun render(
    input: Unit,
    state: Game,
    context: RenderContext<Game, Output>
  ): GameRendering {

    val player = context.renderChild(playerWorkflow, state) { movement ->
      val (newLocation, collided) = state.playerLocation.move(movement, state.board)
      val newState = state.copy(playerLocation = newLocation)
      val output = if (collided) Vibrate else null
      return@renderChild enterState(newState, emittingOutput = output)
    }

    val renderedAis = aiWorkflows.zip(state.aiActors)
        .mapIndexed { index, (aiWorkflow, aiLocation) ->
          val aiInput = Input(state.board, aiLocation)
          val aiCell =
            context.renderChild(aiWorkflow, aiInput, key = index.toString()) { movement ->
              val (newLocation, _) = aiLocation.move(movement, state.board)
              val newState = state.copy(aiActors = state.aiActors.replaceAt(index, newLocation))
              return@renderChild enterState(newState)
            }
          return@mapIndexed aiLocation to aiCell
        }

    val renderedBoard = state.board.withOverlay(
        renderedAis.toMap() + (state.playerLocation to player.avatar)
    )

    return GameRendering(renderedBoard, player)
  }

  override fun snapshotState(state: Game): Snapshot = Snapshot.EMPTY
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

private fun <T> List<T>.replaceAt(
  index: Int,
  newValue: T
): List<T> = mapIndexed { i, t -> if (index == i) newValue else t }
