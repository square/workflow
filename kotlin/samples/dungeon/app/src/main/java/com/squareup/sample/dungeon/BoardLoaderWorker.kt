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

class BoardLoader(
  private val ioDispatcher: CoroutineDispatcher,
  private val assets: AssetManager
) {
  fun load(path: String): Worker<Board> = BoardLoaderWorker(path, ioDispatcher, assets)
}

private class BoardLoaderWorker(
  private val path: String,
  private val ioDispatcher: CoroutineDispatcher,
  private val assets: AssetManager
) : Worker<Board> {

  override fun run(): Flow<Board> = flow {
    val board = coroutineScope {
      // Wait at least a second before emitting to make it look like we're doing real work.
      // Structured concurrency means this coroutineScope block won't return until this delay
      // finishes.
      launch { delay(1000) }

      withContext(ioDispatcher) {
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
