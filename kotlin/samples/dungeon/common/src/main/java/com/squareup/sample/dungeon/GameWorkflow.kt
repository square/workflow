package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.Board.Location
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow

class GameWorkflow : StatefulWorkflow<Unit, Game, Nothing, GameRendering>() {

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
    context: RenderContext<Game, Nothing>
  ): GameRendering {
    println("Rendering ${state.player.name} at ${state.playerLocation}")
    return GameRendering(
        board = state.withPlayer(),
        onEvent = context.onEvent {
          println("Moving ${state.player.name} $it from ${state.playerLocation}")
          it }
    )
  }

  override fun snapshotState(state: Game): Snapshot = Snapshot.EMPTY
}
