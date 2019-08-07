package com.squareup.sample.dungeon

import com.squareup.workflow.WorkflowAction

sealed class GameEvent(private val action: Game.() -> Game) : WorkflowAction<Game, Nothing> {

  object MoveLeft : GameEvent({ copy(playerLocation = playerLocation.left()) })
  object MoveRight : GameEvent({ copy(playerLocation = playerLocation.right()) })
  object MoveUp : GameEvent({ copy(playerLocation = playerLocation.up()) })
  object MoveDown : GameEvent({ copy(playerLocation = playerLocation.down()) })

  override fun invoke(state: Game): Pair<Game, Nothing?> {
    return Pair(action(state), null)
  }

  override fun toString(): String = this::class.simpleName!!
}
