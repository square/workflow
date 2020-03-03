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
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps.PlayingBackAt
import com.squareup.sample.timemachine.TimeMachineWorkflow.TimeMachineProps.Recording
import com.squareup.workflow.Sink
import com.squareup.workflow.Workflow
import com.squareup.workflow.makeEventSink
import com.squareup.workflow.stateful
import com.squareup.workflow.testing.testFromStart
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
class TimeMachineWorkflowTest {

  @Test fun `records and plays back`() {
    data class DelegateRendering(
      val state: String,
      val setState: (String) -> Unit
    )

    val delegateWorkflow = Workflow.stateful<String, Nothing, DelegateRendering>(
        initialState = "initial",
        render = { state ->
          val sink: Sink<String> = makeEventSink { nextState = it }
          DelegateRendering(state, setState = { sink.send(it) })
        }
    )
    val clock = TestTimeSource()
    val tmWorkflow = TimeMachineWorkflow(delegateWorkflow, clock)

    tmWorkflow.testFromStart(Recording(Unit) as TimeMachineProps<Unit>) {
      // Record some renderings.
      awaitNextRendering().let { rendering ->
        assertThat(rendering.value.state).isEqualTo("initial")
        clock += 1.seconds
        rendering.value.setState("second")
      }
      awaitNextRendering().let { rendering ->
        assertThat(rendering.value.state).isEqualTo("second")
      }

      // Play them back.
      sendProps(PlayingBackAt(Unit, Duration.ZERO))
      awaitNextRendering().let { rendering ->
        assertThat(rendering.value.state).isEqualTo("initial")
        assertThat(rendering.totalDuration).isEqualTo(1.seconds)
      }

      clock += 1.seconds
      sendProps(PlayingBackAt(Unit, 1.seconds))
      awaitNextRendering().let { rendering ->
        assertThat(rendering.value.state).isEqualTo("second")
        assertThat(rendering.totalDuration).isEqualTo(1.seconds)

        rendering.value.setState("third")
      }

      // Go back to recording.
      sendProps(Recording(Unit))
      awaitNextRendering().let { rendering ->
        assertThat(rendering.value.state).isEqualTo("third")
        assertThat(rendering.totalDuration).isEqualTo(2.seconds)
      }
    }
  }
}
