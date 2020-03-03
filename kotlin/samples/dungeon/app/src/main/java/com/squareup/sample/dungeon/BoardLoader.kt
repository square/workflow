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
import okio.buffer
import okio.source
import kotlin.time.ExperimentalTime

/**
 * Service class that creates [Worker]s to [load][loadBoard] [Board]s.
 */
@OptIn(ExperimentalTime::class)
class BoardLoader(
  private val ioDispatcher: CoroutineDispatcher,
  private val assets: AssetManager,
  private val boardsAssetPath: String,
  private val delayForFakeLoad: suspend () -> Unit
) {

  private inner class BoardListLoaderWorker : Worker<Map<String, Board>> {
    override fun run(): Flow<Map<String, Board>> = flow {
      val boards = withMinimumDelay() {
        withContext(ioDispatcher) {
          loadBoardsBlocking()
        }
      }
      emit(boards)
    }
  }

  private inner class BoardLoaderWorker(private val filename: String) : Worker<Board> {
    override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
      otherWorker is BoardLoaderWorker && filename == otherWorker.filename

    override fun run(): Flow<Board> = flow {
      val board = withMinimumDelay() {
        withContext(ioDispatcher) {
          loadBoardBlocking(filename)
        }
      }
      emit(board)
    }
  }

  /**
   * Returns a [Worker] that will read and parse all [Board]s located at the [boardsAssetPath]
   * passed into the constructor.
   *
   * @return A map of filenames to the boards loaded from those files.
   */
  fun loadAvailableBoards(): Worker<Map<String, Board>> = BoardListLoaderWorker()

  /**
   * Returns a [Worker] that will read and parse the [Board] located at [filename].
   *
   * Workers created for the same path will be considered [equivalent][Worker.doesSameWorkAs].
   */
  fun loadBoard(filename: String): Worker<Board> = BoardLoaderWorker(filename)

  /**
   * Runs [block] and returns the value it returned, but will not return (by suspending) for at
   * least [delay] period of time. Used to add fake delays to demonstrate loading states.
   */
  private suspend inline fun <T> withMinimumDelay(
    crossinline block: suspend () -> T
  ): T = coroutineScope {
    // Wait at least a second before emitting to make it look like we're doing real work.
    // Structured concurrency means this coroutineScope block won't return until this delay
    // finishes.
    launch { delayForFakeLoad() }

    block()
  }

  @Suppress("UNCHECKED_CAST")
  private fun loadBoardsBlocking(): Map<String, Board> {
    val boardFiles = assets.list(boardsAssetPath)!!.asList()
    return boardFiles.associateWith { filename -> loadBoardBlocking(filename) }
  }

  private fun loadBoardBlocking(filename: String): Board =
    assets.open(absoluteBoardPath(filename))
        .use {
          it.source()
              .buffer()
              .parseBoard()
        }

  private fun absoluteBoardPath(filename: String) = "$boardsAssetPath/$filename"
}
