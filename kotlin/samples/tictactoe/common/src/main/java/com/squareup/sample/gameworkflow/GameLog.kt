/*
 * Copyright 2017 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.gameworkflow

import com.squareup.sample.gameworkflow.GameLog.LogResult
import com.squareup.sample.gameworkflow.GameLog.LogResult.LOGGED
import com.squareup.sample.gameworkflow.GameLog.LogResult.TRY_LATER
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
