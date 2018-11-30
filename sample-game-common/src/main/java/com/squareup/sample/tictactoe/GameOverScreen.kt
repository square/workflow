package com.squareup.sample.tictactoe

import com.squareup.sample.tictactoe.RunGameState.GameOver
import com.squareup.viewbuilder.EventHandlingScreen

data class GameOverScreen(
  override val data: RunGameState.GameOver,
  override val onEvent: (RunGameEvent) -> Unit
) : EventHandlingScreen<GameOver, RunGameEvent>
