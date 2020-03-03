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
package com.squareup.sample.timemachine

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * An immutable, [append]-only list of values that are each associated with increasing timestamps.
 *
 * Timestamps are represented as [Duration]s, relative to some starting point (usually a
 * [TimeMark][kotlin.time.TimeMark], probably from [Clock.markNow][kotlin.time.Clock.markNow]).
 *
 * The timestamp of the last value is exposed as [duration].
 * To get the values without their timestamps, use the [values] sequence.
 * Values can be queried by timestamp using [findValueNearest].
 *
 * @constructor Initializes a [TimeSeries] with a list of increasing value/timestamp pairs.
 */
@ExperimentalTime
internal class TimeSeries<out T>(
  private val data: List<Pair<T, Duration>> = emptyList()
) {

  /**
   * The latest timestamp in the series.
   *
   * This value is _not_ relative to the first value: if the series contains values at times
   * `[1s, 3s]`, this property will return `3s`.
   */
  val duration: Duration get() = data.lastOrNull()?.second ?: Duration.ZERO

  /**
   * A lazy [Sequence] of all the values in the series, without their timestamps.
   */
  val values: Sequence<T> get() = data.asSequence().map { it.first }

  /**
   * Returns a [TimeSeries] with a value appended.
   *
   * @param timestamp The timestamp of the value. Must be greater than or equal to [duration].
   */
  fun append(
    value: @UnsafeVariance T,
    timestamp: Duration
  ): TimeSeries<T> {
    require(timestamp >= duration)
    return TimeSeries(data + (value to timestamp))
  }

  /**
   * If the series is not empty, returns the value whose timestamp is nearest [timestamp].
   *
   * @param timestamp The timestamp to look up. This does not need to exactly match any timestamp
   * contained in the series. If the series has at least one element, this method will always
   * return a value.
   * @throws NoSuchElementException If the series is empty.
   */
  fun findValueNearest(timestamp: Duration): T {
    if (data.isEmpty()) throw NoSuchElementException("TimeSeries is empty")

    val index = data.binarySearchBy(timestamp) { it.second }
    if (index >= 0) {
      // Exact match was found.
      return data[index].first
    }

    // Index is -(insertion point). We need to do our own calculation to decide which side of the
    // insertion point is closer. We consider the "insertion point" to be the "right" index, since
    // it would be on the right of the inserted value after insertion.
    val rightIndex = (-index).coerceAtMost(data.size - 1)
    val leftIndex = (rightIndex - 1).coerceAtLeast(0)
    val rightTimestamp = data[rightIndex].second
    val leftTimestamp = data[leftIndex].second
    val rightDistance = rightTimestamp - timestamp
    val leftDistance = timestamp - leftTimestamp

    return when {
      rightDistance < leftDistance -> data[rightIndex]
      else -> data[leftIndex]
    }.first
  }
}
