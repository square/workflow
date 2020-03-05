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

import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps
import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps.PlaybackAt
import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps.RecordValue
import com.squareup.sample.timemachine.RecorderWorkflow.Recording
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A workflow that will either record values or play them back, depending on the mode selected by
 * the [props][RecorderProps].
 *
 * When [recording][RecordValue], the [RecordValue.value] will be recorded at the current
 * time and returned in the [rendering][TimeMachineRendering].
 *
 * When [playing back][PlaybackAt], the recorded value closest to [PlaybackAt.timestamp] will be
 * returned in the [rendering][TimeMachineRendering].
 */
@ExperimentalTime
internal class RecorderWorkflow<T>(
  private val clock: TimeSource
) : StatefulWorkflow<RecorderProps<T>, Recording<T>, Nothing, TimeMachineRendering<T>>() {

  /**
   * Sealed class that controls whether a [RecorderWorkflow] is [recording][RecordValue] or
   * [playing back][PlaybackAt] values.
   */
  sealed class RecorderProps<out T> {
    /**
     * Record [value] at the current time and return it in the rendering.
     */
    data class RecordValue<out T>(val value: T) : RecorderProps<T>()

    /**
     * Return the recorded value closest to [timestamp] in the [rendering][TimeMachineRendering].
     */
    data class PlaybackAt(val timestamp: Duration) : RecorderProps<Nothing>()
  }

  data class Recording<out T>(
    val startTime: TimeMark,
    val series: TimeSeries<T>
  )

  override fun initialState(
    props: RecorderProps<T>,
    snapshot: Snapshot?
  ): Recording<T> = Recording(
      startTime = clock.markNow(),
      series = TimeSeries()
  )

  override fun onPropsChanged(
    old: RecorderProps<T>,
    new: RecorderProps<T>,
    state: Recording<T>
  ): Recording<T> {
    if (new is RecordValue) {
      val timestamp = state.startTime.elapsedNow()
      return state.copy(series = state.series.append(new.value, timestamp))
    }
    return state
  }

  override fun render(
    props: RecorderProps<T>,
    state: Recording<T>,
    context: RenderContext<Recording<T>, Nothing>
  ): TimeMachineRendering<T> {
    val value = when (props) {
      is RecordValue -> props.value
      is PlaybackAt -> state.series.findValueNearest(props.timestamp)
    }

    return TimeMachineRendering(
        value = value,
        totalDuration = state.series.duration
    )
  }

  override fun snapshotState(state: Recording<T>): Snapshot = Snapshot.EMPTY
}
