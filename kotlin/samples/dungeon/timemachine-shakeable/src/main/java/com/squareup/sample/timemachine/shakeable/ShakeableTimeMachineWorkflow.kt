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
package com.squareup.sample.timemachine.shakeable

import android.content.Context
import com.squareup.sample.timemachine.TimeMachineWorkflow
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineWorkflow.PropsFactory
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineWorkflow.State
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineWorkflow.State.PlayingBack
import com.squareup.sample.timemachine.shakeable.ShakeableTimeMachineWorkflow.State.Recording
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.action
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * A wrapper around a [TimeMachineWorkflow] that uses [ShakeableTimeMachineLayoutRunner] to render
 * the [delegate workflow][TimeMachineWorkflow.delegateWorkflow]'s rendering, but wrap it in a
 * UI to scrub around the recorded timeline when the device is shaken.
 *
 * This workflow takes a [PropsFactory] as its props. See that class for more documentation.
 */
@ExperimentalTime
class ShakeableTimeMachineWorkflow<in P, O : Any, out R : Any>(
  private val timeMachineWorkflow: TimeMachineWorkflow<P, O, R>,
  context: Context
) : StatefulWorkflow<PropsFactory<P>, State, O, ShakeableTimeMachineRendering>() {

  /**
   * A factory that knows how to create the props for a [TimeMachineWorkflow.delegateWorkflow],
   * given a flag indicating whether the workflow is in recording mode or not.
   */
  class PropsFactory<out P>(
    val createDelegateProps: (recording: Boolean) -> P
  )

  sealed class State {
    object Recording : State()
    data class PlayingBack(val timestamp: Duration) : State()
  }

  private val shakeWorker = ShakeWorker(context)

  override fun initialState(
    props: PropsFactory<P>,
    snapshot: Snapshot?
  ): State = Recording

  override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

  override fun render(
    props: PropsFactory<P>,
    state: State,
    context: RenderContext<State, O>
  ): ShakeableTimeMachineRendering {
    // Only listen to shakes when recording.
    if (state === Recording) context.runningWorker(shakeWorker) { onShake }

    val delegateProps = props.createDelegateProps(state === Recording)

    val timeMachineProps = when (state) {
      Recording -> TimeMachineProps.Recording(delegateProps)
      is PlayingBack -> TimeMachineProps.PlayingBackAt(delegateProps, state.timestamp)
    }

    val timeMachineRendering =
      context.renderChild(timeMachineWorkflow, timeMachineProps) { output: O ->
        forwardOutput(output)
      }

    return ShakeableTimeMachineRendering(
        rendering = timeMachineRendering.value,
        totalDuration = timeMachineRendering.totalDuration,
        playbackPosition = if (state is PlayingBack) {
          minOf(state.timestamp, timeMachineRendering.totalDuration)
        } else {
          Duration.INFINITE
        },
        recording = (state is Recording),
        onSeek = when (state) {
          Recording -> {
            // No handler. Need the _ so the type inferencer doesn't get confused for the lambda
            // below. This will be fixed with the new type inference algorithm in 1.4.
            { _ -> }
          }
          is PlayingBack -> {
            { position -> context.actionSink.send(SeekAction(position)) }
          }
        },
        onResumeRecording = when (state) {
          Recording -> {
            // No handler.
            {}
          }
          is PlayingBack -> {
            { context.actionSink.send(ResumeRecordingAction()) }
          }
        }
    )
  }

  private val onShake = action {
    nextState = PlayingBack(Duration.INFINITE)
  }

  private inner class SeekAction(
    private val newPosition: Duration
  ) : WorkflowAction<State, O> {
    override fun Updater<State, O>.apply() {
      nextState = PlayingBack(newPosition)
    }
  }

  private inner class ResumeRecordingAction : WorkflowAction<State, O> {
    override fun Updater<State, O>.apply() {
      nextState = Recording
    }
  }

  private fun forwardOutput(output: O) = action { setOutput(output) }
}
