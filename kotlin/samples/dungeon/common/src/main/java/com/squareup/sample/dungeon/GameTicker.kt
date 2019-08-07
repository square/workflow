package com.squareup.sample.dungeon

import com.squareup.workflow.Worker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Provides the heartbeat for the game. */
class GameTicker(periodMs: Int = 1000 / 15) {

  private class TickerWorker(private val periodMs: Int) : Worker<Long> {
    override fun run(): Flow<Long> = flow {
      val count = 0L
      while (true) {
        emit(count)
        delay(periodMs.toLong())
      }
    }

    override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean {
      return (otherWorker is TickerWorker) && (otherWorker.periodMs == periodMs)
    }
  }

  val ticks: Worker<Long> = TickerWorker(periodMs)
}
