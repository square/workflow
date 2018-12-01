package com.squareup.sample.tictactoe

import com.squareup.sample.tictactoe.GameLog.LogResult
import com.squareup.sample.tictactoe.GameLog.LogResult.LOGGED
import com.squareup.sample.tictactoe.GameLog.LogResult.TRY_LATER
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.Single.just
import java.util.concurrent.TimeUnit.SECONDS

/**
 * "Saves" game state, to demonstrate using services from a workflow.
 * Actually just reports success or failure, usually the latter.
 */
interface GameLog {
  enum class LogResult {
    TRY_LATER,
    LOGGED
  }

  fun logGame(game: CompletedGame): Single<LogResult>
}

class RealGameLog(
  private val mainThread: Scheduler
) : GameLog {
  private var attempt = 1

  override fun logGame(game: CompletedGame): Single<LogResult> {
    return if (attempt++ % 3 == 0) {
      just(LOGGED)
    } else {
      just(TRY_LATER)
    }.delay(1, SECONDS)
        .observeOn(mainThread)
  }
}
