package com.squareup.sample.dungeon

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
import java.util.EnumSet

class GameWorkflow(
  private val playerWorkflow: PlayerWorkflow
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
  ) = Game(
      board = Board(
          width = 16,
          height = 16,
          cells = Board.EMPTY
      ),

      player = Player("zach", BoardCell("👩🏻‍🎤")),
      playerLocation = Location(8, 8)
  )

  override fun render(
    input: Unit,
    state: Game,
    context: RenderContext<Game, Output>
  ): GameRendering {

    return GameRendering(
        board = state.withPlayer(),
        player = context.renderChild(playerWorkflow, state) { movePlayerDirections ->
          var collisionDetected = false
          val newState = state.movePlayer(movePlayerDirections) { collisionDetected = true }
          val output = if (collisionDetected) Vibrate else null
          enterState(newState, emittingOutput = output)
        }
    )
  }

  override fun snapshotState(state: Game): Snapshot = Snapshot.EMPTY

  private fun Game.movePlayer(
    directions: EnumSet<Direction>,
    onCollisionDetected: () -> Unit
  ): Game {
    var (x, y) = playerLocation
    if (LEFT in directions) x -= 1
    if (RIGHT in directions) x += 1
    // Don't let the player leave the board.
    x = x.coerceIn(0 until board.width)
    // Don't allow collisions with obstacles on the board.
    if (!board[x, y].isEmpty) {
      onCollisionDetected()
      x = playerLocation.x
    }

    if (UP in directions) y -= 1
    if (DOWN in directions) y += 1
    y = y.coerceIn(0 until board.height)
    if (!board[x, y].isEmpty) {
      onCollisionDetected()
      y = playerLocation.y
    }

    val newLocation = Location(x, y)
    println("Moved player ${player.cell} to $newLocation")

    return copy(playerLocation = newLocation)
  }
}
