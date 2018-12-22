/*
 * Copyright 2017 Square Inc.
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
package com.squareup.sample.authgameapp

import com.squareup.sample.authgameapp.ShellState.Authenticating
import com.squareup.sample.authgameapp.ShellState.RunningGame
import com.squareup.sample.authworkflow.AuthRenderer
import com.squareup.sample.tictactoe.ConfirmQuitScreen
import com.squareup.sample.tictactoe.RunGameRenderer
import com.squareup.viewbuilder.StackedMainAndModalScreen
import com.squareup.viewbuilder.toMainAndModal
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool

/**
 * TODO(rjrjr): it's weird and distracting that the specific modal type (ConfirmQuitScreen) leaks
 * all the way up here, but it'll do until we have a more general Alert story.
 */
object ShellRenderer :
    Renderer<ShellState, Nothing, StackedMainAndModalScreen<*, ConfirmQuitScreen>> {
  override fun render(
    state: ShellState,
    workflow: WorkflowInput<Nothing>,
    workflows: WorkflowPool
  ): StackedMainAndModalScreen<*, ConfirmQuitScreen> {
    return when (state) {
      is Authenticating ->
        AuthRenderer.render(
            state.authWorkflow.state,
            workflows.input(state.authWorkflow),
            workflows
        ).toMainAndModal()

      is RunningGame -> RunGameRenderer.render(
          state.runGameWorkflow.state,
          workflows.input(state.runGameWorkflow),
          workflows
      )
    }
  }
}
