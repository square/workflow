package com.squareup.sample.dungeon

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import kotlin.streams.toList

private const val WALL = "🌳"
private const val SPACE = "\u00A0"

class DungeonAppWorkflow : StatefulWorkflow<Unit, Unit, Nothing, Any>() {
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
    return BoardRendering(
        width = 16,
        height = 16,
        cells = ((WALL.repeat(16)) +
            (WALL + SPACE.repeat(14) + WALL).repeat(14) +
            (WALL.repeat(16)))
            .codePoints()
            .toList()
            .map(::BoardCell)
    )
  }

  override fun snapshotState(state: Unit): Snapshot = Snapshot.EMPTY
}
