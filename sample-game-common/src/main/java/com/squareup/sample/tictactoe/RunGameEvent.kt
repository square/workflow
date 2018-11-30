package com.squareup.sample.tictactoe

/**
 * The events accepted by [RunGameReactor].
 */
sealed class RunGameEvent {
  data class StartGame(
    val x: String,
    val o: String
  ) : RunGameEvent()

  object ConfirmQuit : RunGameEvent()

  object ContinuePlaying : RunGameEvent()

  object PlayAgain : RunGameEvent()

  object NoMore : RunGameEvent()

  object TrySaveAgain : RunGameEvent()
}
