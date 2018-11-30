package com.squareup.sample.tictactoe

import com.squareup.viewbuilder.EventHandlingScreen

data class NewGameScreen(
  override val onEvent: (RunGameEvent) -> Unit
) : EventHandlingScreen<Unit, RunGameEvent> {
  override val data = Unit
}
