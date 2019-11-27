/*
 * Copyright 2019 Square Inc.
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

import android.content.res.AssetManager
import com.squareup.sample.dungeon.board.Board
import com.squareup.sample.dungeon.board.parseBoard
import com.squareup.workflow.Worker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.source

/**
 * Service class that creates [Worker]s to [load][load] [Board]s.
 */
class BoardLoader(
  private val ioDispatcher: CoroutineDispatcher,
  private val assets: AssetManager
) {

  /**
   * Returns a [Worker] that will read and parse the [Board] located at [path].
   *
   * Workers created for the same path will be considered [equivalent][Worker.doesSameWorkAs].
   */
  fun load(path: String): Worker<Board> = BoardLoaderWorker(path)

  private inner class BoardLoaderWorker(private val path: String) : Worker<Board> {

    override fun run(): Flow<Board> = flow {
      val board = coroutineScope {
        // Wait at least a second before emitting to make it look like we're doing real work.
        // Structured concurrency means this coroutineScope block won't return until this delay
        // finishes.
        launch { delay(1000) }

        withContext(ioDispatcher) {
          @Suppress("BlockingMethodInNonBlockingContext")
          assets.open(path)
              .use {
                it.source()
                    .parseBoard()
              }
        }
      }
      emit(board)
    }

    override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean {
      return otherWorker is BoardLoaderWorker && path == otherWorker.path
    }
  }
}
