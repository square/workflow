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
