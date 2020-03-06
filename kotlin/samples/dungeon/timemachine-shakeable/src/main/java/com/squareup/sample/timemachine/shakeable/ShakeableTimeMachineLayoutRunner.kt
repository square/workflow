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

import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.transition.TransitionManager
import com.squareup.sample.timemachine.shakeable.internal.GlassFrameLayout
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.WorkflowViewStub
import com.squareup.workflow.ui.backPressedHandler
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

/**
 * Renders [ShakeableTimeMachineWorkflow][ShakeableTimeMachineWorkflow]
 * [renderings][ShakeableTimeMachineRendering].
 */
@OptIn(ExperimentalTime::class)
class ShakeableTimeMachineLayoutRunner(
  private val view: View
) : LayoutRunner<ShakeableTimeMachineRendering> {

  private val glassView: GlassFrameLayout = view.findViewById(R.id.glass_view)
  private val childStub: WorkflowViewStub = view.findViewById(R.id.child_stub)
  private val seek: SeekBar = view.findViewById(R.id.time_travel_seek)
  private val group: Group = view.findViewById(R.id.group)
  private val endLabel: TextView = view.findViewById(R.id.end_label)
  private var wasRecording: Boolean? = null

  init {
    val startLabel = view.findViewById<TextView>(R.id.start_label)
    startLabel.text = Duration.ZERO.toUiString()
  }

  override fun showRendering(
    rendering: ShakeableTimeMachineRendering,
    viewEnvironment: ViewEnvironment
  ) {
    // Only handle back presses explicitly if in playback mode.
    view.backPressedHandler = rendering.onResumeRecording.takeUnless { rendering.recording }

    seek.max = rendering.totalDuration.toProgressInt()
    seek.progress = rendering.playbackPosition.toProgressInt()

    endLabel.text = rendering.totalDuration.toUiString()

    seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(
        seekBar: SeekBar,
        progress: Int,
        fromUser: Boolean
      ) {
        if (!fromUser) return
        rendering.onSeek(progress.toProgressDuration())
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {
        // Don't care.
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        // Don't care.
      }
    })

    if (wasRecording != rendering.recording) {
      wasRecording = rendering.recording

      val visibility = if (rendering.recording) View.GONE else View.VISIBLE
      TransitionManager.beginDelayedTransition(view as ViewGroup)
      group.visibility = visibility
      glassView.blockTouchEvents = !rendering.recording
    }

    // Show the child screen.
    childStub.update(rendering.rendering, viewEnvironment)
  }

  private fun Duration.toProgressInt(): Int = toLongMilliseconds().toInt()
  private fun Int.toProgressDuration(): Duration = milliseconds

  private fun Duration.toUiString(): String = toString()

  companion object : ViewFactory<ShakeableTimeMachineRendering> by bind(
      R.layout.shakeable_time_machine_layout, ::ShakeableTimeMachineLayoutRunner
  )
}
