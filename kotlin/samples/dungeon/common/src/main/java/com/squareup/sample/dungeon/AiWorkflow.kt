package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.AiWorkflow.Input
import com.squareup.sample.dungeon.AiWorkflow.State
import com.squareup.sample.dungeon.Board.Location
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.onWorkerOutput
import kotlin.random.Random
import kotlin.reflect.KClass

class AiWorkflow(
  private val avatar: BoardCell = BoardCell("👻"),
  private val random: Random,
  private val ticker: GameTicker,
  private val speedFactor: Int = random.nextInt(2, 4)
) : StatefulWorkflow<Input, State, Movement, BoardCell>() {

  data class Input(
    val board: Board,
    val myLocation: Location
  )

  data class State(val direction: Direction)

  override fun initialState(
    input: Input,
    snapshot: Snapshot?
  ): State {
    val startingDirection = random.nextEnum(Direction::class)
    return State(startingDirection)
  }

  override fun render(
    input: Input,
    state: State,
    context: RenderContext<State, Movement>
  ): BoardCell {
    // Moves in a square.
    context.onWorkerOutput(ticker.ticks) { tick ->
      val newState = if (tick % random.nextInt(2, 5) == 0L) {
        // Rotate 90 degrees.
        val newDirection = when (state.direction) {
          UP -> RIGHT
          RIGHT -> DOWN
          DOWN -> LEFT
          LEFT -> UP
        }
        println("AI changing direction from ${state.direction} to $newDirection")
        state.copy(direction = newDirection)
      } else state

      // Move slower.
      val shouldMove = tick % speedFactor == 0L
      val movement = if (shouldMove) Movement(newState.direction) else Movement()

      return@onWorkerOutput enterState(newState, emittingOutput = movement)
    }

    return avatar
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY
}

private fun <T : Enum<T>> Random.nextEnum(enumClass: KClass<T>): T {
  val values = enumClass.java.enumConstants
  return values[nextInt(values.size)]
}
