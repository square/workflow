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

import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps.PlaybackAt
import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps.RecordValue
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps.PlayingBackAt
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps.Recording
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
import com.squareup.workflow.renderChild
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * A workflow that will continuously render [delegateWorkflow] and either record its renderings, or
 * play them back via the [rendering][TimeMachineRendering] depending on the mode selected by this
 * workflow's [props][TimeMachineProps].
 *
 * Any outputs from the [delegateWorkflow] will be forwarded as-is, even in playback mode.
 *
 * @param delegateWorkflow The [Workflow] whose renderings to record. This workflow will be rendered
 * continuously as long as the `TimeMachineWorkflow` is being rendered.
 * @param clock The [Clock] to use to assign timestamps to recorded values.
 */
@ExperimentalTime
class TimeMachineWorkflow<P, O : Any, out R>(
  private val delegateWorkflow: Workflow<P, O, R>,
  clock: TimeSource
) : StatelessWorkflow<TimeMachineProps<P>, O, TimeMachineRendering<R>>() {

  /**
   * Sealed class that determines whether a [TimeMachineWorkflow] is [recording][Recording] the
   * [delegate workflow's][delegateWorkflow] renderings, or [playing them back][PlayingBackAt].
   */
  sealed class TimeMachineProps<out P> {
    /**
     * The props to pass to the [delegateWorkflow].
     */
    abstract val delegateProps: P

    /**
     * Puts the [TimeMachineWorkflow] in recording mode.
     *
     * The rendering returned by the [delegateWorkflow] will be recorded.
     *
     * @param delegateProps The props to pass to the [delegateWorkflow].
     */
    data class Recording<out P>(override val delegateProps: P) : TimeMachineProps<P>()

    /**
     * Puts the [TimeMachineWorkflow] in playback mode.
     *
     * The [delegateWorkflow] will still be rendered, but that rendering will be dropped.
     *
     * @param delegateProps The props to pass to the [delegateWorkflow].
     * @param timestamp The timestamp nearest to the recorded value to return in the
     * [TimeMachineRendering].
     */
    data class PlayingBackAt<out P>(
      override val delegateProps: P,
      val timestamp: Duration
    ) : TimeMachineProps<P>()
  }

  private val recordingWorkflow = RecorderWorkflow<R>(clock)

  override fun render(
    props: TimeMachineProps<P>,
    context: RenderContext<Nothing, O>
  ): TimeMachineRendering<R> {
    // Always render the delegate, even if in playback mode, to keep it alive.
    val delegateRendering =
      context.renderChild(delegateWorkflow, props.delegateProps) { forwardOutput(it) }

    val recorderProps = when (props) {
      is Recording -> RecordValue(delegateRendering)
      is PlayingBackAt -> PlaybackAt(props.timestamp)
    }

    return context.renderChild(recordingWorkflow, recorderProps)
  }

  private fun forwardOutput(output: O) = action { setOutput(output) }
}
