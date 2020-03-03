/*
 * Copyright 2020 Square Inc.
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

package com.squareup.sample.dungeon

import android.content.Context
import kotlinx.coroutines.delay
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Interface for TestApplication to implement to configure fake loading delay for UI tests.
 */
interface DungeonApplication {
  suspend fun delayForFakeLoad()
}

/**
 * Retrieves the loading delay from the application if it is a [DungeonApplication], or else a
 * default.
 */
@OptIn(ExperimentalTime::class)
suspend fun Context.delayForFakeLoad() =
  (applicationContext as? DungeonApplication)?.delayForFakeLoad()
      ?: delay(1.seconds.toLongMilliseconds())
