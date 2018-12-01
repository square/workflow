package com.squareup.sample.tictactoe

import com.squareup.viewbuilder.EventHandlingScreen

data class GamePlayScreen(
  override val data: Turn,
  override val onEvent: (TakeTurnsEvent) -> Unit
) : EventHandlingScreen<Turn, TakeTurnsEvent>
