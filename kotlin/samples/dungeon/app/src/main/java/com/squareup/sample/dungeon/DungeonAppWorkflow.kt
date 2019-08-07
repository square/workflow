package com.squareup.sample.dungeon

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.renderChild

class DungeonAppWorkflow(
  private val gameWorkflow: GameWorkflow
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
    return context.renderChild(gameWorkflow)
  }

  override fun snapshotState(state: Unit): Snapshot = Snapshot.EMPTY
}
