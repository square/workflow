package com.squareup.sample.dungeon

import android.os.Vibrator
import com.squareup.sample.dungeon.GameWorkflow.Output.PlayerWasEaten
import com.squareup.sample.dungeon.GameWorkflow.Output.Vibrate
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.renderChild

class DungeonAppWorkflow(
  private val gameWorkflow: GameWorkflow,
  private val vibrator: Vibrator
) : StatefulWorkflow<Unit, Unit, Nothing, Any>() {

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ) {
  }

  override fun render(
    input: Unit,
    state: Unit,
    context: RenderContext<Unit, Nothing>
  ): Any {
    return context.renderChild(gameWorkflow) { output ->
      when (output) {
        Vibrate -> vibrate(50)
        PlayerWasEaten -> {
          vibrate(20)
          vibrate(20)
          vibrate(20)
          vibrate(20)
          vibrate(1000)
        }
      }
      noAction()
    }
  }

  override fun snapshotState(state: Unit): Snapshot = Snapshot.EMPTY

  private fun vibrate(durationMs: Long) {
    @Suppress("DEPRECATION")
    vibrator.vibrate(durationMs)
  }
}
