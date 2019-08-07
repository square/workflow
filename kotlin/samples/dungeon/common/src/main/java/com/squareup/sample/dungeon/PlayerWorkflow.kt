package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.PlayerWorkflow.Event.StartMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Event.StopMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Rendering
import com.squareup.sample.dungeon.PlayerWorkflow.State
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.onWorkerOutput
import java.util.EnumSet

class PlayerWorkflow(
  private val ticker: GameTicker
) : StatefulWorkflow<Game, State, EnumSet<Direction>, Rendering>() {

  sealed class Event {
    data class StartMoving(val direction: Direction) : Event()
    data class StopMoving(val direction: Direction) : Event()
  }

  data class State(val movement: EnumSet<Direction> = EnumSet.noneOf(Direction::class.java))

  /**
   * @param onEvent Call to change the directions the player is currently moving.
   */
  data class Rendering(val onEvent: (Event) -> Unit)

  override fun initialState(
    input: Game,
    snapshot: Snapshot?
  ): State = State()

  override fun render(
    input: Game,
    state: State,
    context: RenderContext<State, EnumSet<Direction>>
  ): Rendering {
    context.onWorkerOutput(ticker.ticks) {
      println("tick: $it, moving player ${state.movement}")
      emitOutput("move player ${state.movement}", state.movement)
    }

    return Rendering(onEvent = context.onEvent { event ->
      val newMovement = state.movement.clone()
      when (event) {
        is StartMoving -> newMovement.add(event.direction)
        is StopMoving -> newMovement.remove(event.direction)
      }
      enterState(state.copy(movement = newMovement))
    })
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY
}
