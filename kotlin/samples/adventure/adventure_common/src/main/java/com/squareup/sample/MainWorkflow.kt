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
@file:Suppress("NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE")

package com.squareup.sample

import com.squareup.sample.MainWorkflow.Finished
import com.squareup.sample.NavigationOutput.GoForward
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.onWorkerOutput
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import com.squareup.workflow.ui.BackStackScreen

typealias ScreenOne = ScreenWorkflow<String, Nothing, String, String>

typealias ScreenTwo = ScreenWorkflow<String, Nothing, String, String>

typealias ScreenThree = SimpleScreenWorkflow<String, Nothing, String, String>

class MainWorkflow(
  private val wf1: ScreenOne,
  private val wf2: ScreenTwo,
  private val wf3: ScreenThree
) : StatelessWorkflow<String, Finished, BackStackScreen<String>>() {

  object Finished

  override fun render(
    input: String,
    context: RenderContext<Nothing, Finished>
  ) = context.renderScreens(goBackAction = emitOutput(Finished)) {
    val wf1R = renderChild(wf1, input) { noop() }
    val wf2R = renderChild(wf2, wf1R) { noop() }
    return@renderScreens renderChild(wf3, wf2R) { noop() }
  }.display
}

class RealScreenOne : ScreenOne by Workflow.stateless({ screenInput ->
  ScreenRendering(
      screenRendering = "input: ${screenInput.input}",
      display = "input: ${screenInput.input}",
      goBackHandler = screenInput.goBackHandler
  )
})

class RealScreenTwo : ScreenTwo by Workflow.stateful(
    initialState = { input, _ -> "input: $input" },
    snapshotState = { Snapshot.EMPTY },
    render = { input, state ->
      ScreenRendering(
          screenRendering = state,
          display = "input: $input, state: $state",
          goBackHandler = input.goBackHandler
      )
    }
)

class RealScreenThree(
  private val worker: Worker<String>
) : ScreenThree by Workflow.stateless({ screenInput ->
  onWorkerOutput(worker) { emitOutput(GoForward()) }
  // Do something with this.
  val buttonClicked: () -> Unit = screenInput.goBackHandler

  return@stateless Pair("input: $screenInput", "input: $screenInput")
})
