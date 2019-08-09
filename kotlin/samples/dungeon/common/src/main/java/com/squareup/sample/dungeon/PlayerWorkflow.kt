package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.PlayerWorkflow.Event.StartMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Event.StopMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Rendering
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.onWorkerOutput

class PlayerWorkflow(
  private val avatar: BoardCell = BoardCell("👩🏻‍🎤"),
  private val ticker: GameTicker
) : StatefulWorkflow<Game, Movement, Movement, Rendering>() {

  sealed class Event {
    data class StartMoving(val direction: Direction) : Event()
    data class StopMoving(val direction: Direction) : Event()
  }

  /**
   * @param onEvent Call to change the directions the player is currently moving.
   */
  data class Rendering(
    val avatar: BoardCell,
    val onEvent: (Event) -> Unit
  )

  override fun initialState(
    input: Game,
    snapshot: Snapshot?
  ): Movement = Movement()

  override fun render(
    input: Game,
    state: Movement,
    context: RenderContext<Movement, Movement>
  ): Rendering {
    context.onWorkerOutput(ticker.ticks) {
      println("tick: $it, moving player $state")
      emitOutput("move player $state", state)
    }

    return Rendering(
        avatar = avatar,
        onEvent = context.onEvent { event ->
          val newMovement = when (event) {
            is StartMoving -> state + event.direction
            is StopMoving -> state - event.direction
          }
          enterState(newMovement)
        })
  }

  override fun snapshotState(state: Movement): Snapshot = Snapshot.EMPTY
}
