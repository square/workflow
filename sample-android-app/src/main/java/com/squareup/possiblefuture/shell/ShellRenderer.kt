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
package com.squareup.possiblefuture.shell

import com.squareup.possiblefuture.authworkflow.AuthRenderer
import com.squareup.possiblefuture.shell.ShellState.Authenticating
import com.squareup.possiblefuture.shell.ShellState.RunningGame
import com.squareup.sample.tictactoe.RunGameRenderer
import com.squareup.viewbuilder.ViewStackScreen
import com.squareup.workflow.Renderer
import com.squareup.workflow.WorkflowInput
import com.squareup.workflow.WorkflowPool

/**
 * Todo: proper modal handling and
 *
 *     <ShellState, Nothing, MainAndModalScreen<ViewStackScreen<*>, *>>>
 */
object ShellRenderer : Renderer<ShellState, Nothing, ViewStackScreen<*>> {
  override fun render(
    state: ShellState,
    workflow: WorkflowInput<Nothing>,
    workflows: WorkflowPool
  ): ViewStackScreen<*> {
    return when (state) {
      is Authenticating -> AuthRenderer.render(
          state.delegateState,
          workflows.input(state.id),
          workflows
      ).let { ViewStackScreen(it) }

      is RunningGame -> RunGameRenderer.render(
          state.delegateState,
          workflows.input(state.id),
          workflows
      ).let { modal ->
        // Can't do dialogs yet, so just show the top-most modal if there is one.
        modal.modals.lastOrNull()
            ?.let { ViewStackScreen(it) }
            ?: ViewStackScreen(modal.main)
      }
    }
  }
}
