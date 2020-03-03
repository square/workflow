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

import com.google.common.truth.Truth.assertThat
import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps.PlaybackAt
import com.squareup.sample.timemachine.RecorderWorkflow.RecorderProps.RecordValue
import com.squareup.sample.timemachine.RecorderWorkflow.Recording
import com.squareup.workflow.testing.renderTester
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
class RecorderWorkflowTest {

  @Test fun `onPropsChanged records value when recording`() {
    val clock = TestTimeSource()
    val workflow = RecorderWorkflow<String>(clock)
    val startTime = clock.markNow()
    clock += 42.milliseconds

    val newState = workflow.onPropsChanged(
        old = RecordValue("foo"),
        new = RecordValue("bar"),
        state = Recording(
            startTime = startTime,
            series = TimeSeries(listOf("foo" to 0.milliseconds))
        )
    )

    assertThat(newState.series.duration).isEqualTo(42.milliseconds)
    assertThat(newState.series.values.toList()).isEqualTo(listOf("foo", "bar"))
  }

  @Test fun `onPropsChanged doesn't record value when not recording`() {
    val clock = TestTimeSource()
    val workflow = RecorderWorkflow<String>(clock)
    val startTime = clock.markNow()
    clock += 42.milliseconds

    val newState = workflow.onPropsChanged(
        old = RecordValue("foo"),
        new = PlaybackAt(42.milliseconds),
        state = Recording(
            startTime = startTime,
            series = TimeSeries(listOf("foo" to 0.milliseconds))
        )
    )

    assertThat(newState.series.duration).isEqualTo(0.milliseconds)
    assertThat(newState.series.values.toList()).isEqualTo(listOf("foo"))
  }

  @Test fun `render returns recorded value when recording`() {
    val clock = TestTimeSource()
    val workflow = RecorderWorkflow<String>(clock)
    val startTime = clock.markNow()

    workflow
        .renderTester(
            props = RecordValue("bar"),
            initialState = Recording(
                startTime = startTime,
                series = TimeSeries(listOf("foo" to 42.milliseconds))
            )
        )
        .render { rendering ->
          assertThat(rendering.value).isEqualTo("bar")
        }
  }

  @Test fun `render returns recorded value when playing back`() {
    val clock = TestTimeSource()
    val workflow = RecorderWorkflow<String>(clock)
    val startTime = clock.markNow()

    workflow
        .renderTester(
            props = PlaybackAt(10.milliseconds),
            initialState = Recording(
                startTime = startTime,
                series = TimeSeries(
                    listOf(
                        "foo" to 0.milliseconds,
                        "bar" to 42.milliseconds
                    )
                )
            )
        )
        .render { rendering ->
          assertThat(rendering.value).isEqualTo("foo")
        }
  }
}
