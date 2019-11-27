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

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * @param rendering The rendering of the
 * [delegate workflow][com.squareup.sample.timemachine.TimeMachineWorkflow.delegateWorkflow].
 * @param totalDuration The total duration of the recorded session.
 * @param playbackPosition The timestamp of the rendering currently being played back, or
 * [Duration.INFINITE] if not recording.
 * @param recording If false, the time travelling UI should be shown.
 * @param onSeek Event handler that will be called when [recording] is false and the timeline is
 * scrubbed.
 * @param onResumeRecording Event handler that will be called when [recording] is false and the user
 * wants to go back to the live delegate workflow.
 */
@ExperimentalTime
data class ShakeableTimeMachineRendering(
  val rendering: Any,
  val totalDuration: Duration,
  val playbackPosition: Duration,
  val recording: Boolean,
  val onSeek: (Duration) -> Unit = {},
  val onResumeRecording: () -> Unit
)
