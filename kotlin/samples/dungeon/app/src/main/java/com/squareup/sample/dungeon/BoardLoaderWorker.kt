package com.squareup.sample.dungeon

import android.content.res.AssetManager
import com.squareup.sample.dungeon.board.Board
import com.squareup.sample.dungeon.board.parseBoard
import com.squareup.workflow.Worker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    emit(withContext(ioDispatcher) {
      assets.open(path)
          .use {
            it.source()
                .parseBoard()
          }
    })
  }

  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean {
    return otherWorker is BoardLoaderWorker && path == otherWorker.path
  }
}
