package com.squareup.possiblefuture.shell

import com.squareup.possiblefuture.authworkflow.AuthReactor
import com.squareup.possiblefuture.shell.ShellState.Authenticating
import com.squareup.possiblefuture.shell.ShellState.RunningGame
import com.squareup.sample.tictactoe.RunGameReactor
import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.register
import com.squareup.workflow.rx2.EventChannel
import com.squareup.workflow.rx2.Reactor
import com.squareup.workflow.rx2.doLaunch
import com.squareup.workflow.rx2.nextDelegateReaction
import io.reactivex.Single

/**
 * Application specific root [Workflow], and demonstration of workflow composition.
 * We log in, and then play as many games as we want.
 *
 * Delegates to [AuthReactor] and [RunGameReactor]. Responsible only for deciding
 * what to do as each nested workflow ends.
 */
typealias ShellWorkflow = Workflow<ShellState, Nothing, Unit>

/**
 * Only event handled by [ShellReactor].
 */
object LogOut

internal class ShellReactor(
  private val runGameReactor: RunGameReactor,
  private val authReactor: AuthReactor
) : Reactor<ShellState, LogOut, Unit> {

  override fun launch(
    initialState: ShellState,
    workflows: WorkflowPool
  ): Workflow<ShellState, LogOut, Unit> {
    workflows.register(runGameReactor)
    workflows.register(authReactor)

    return doLaunch(initialState, workflows)
  }

  override fun onReact(
    state: ShellState,
    events: EventChannel<LogOut>,
    workflows: WorkflowPool
  ): Single<out Reaction<ShellState, Unit>> = when (state) {
    is Authenticating -> workflows.nextDelegateReaction(state).map {
      when (it) {
        is EnterState -> EnterState<ShellState>(state.copy(delegateState = it.state))
        is FinishWith -> EnterState(RunningGame())
      }
    }

    is RunningGame -> events.select {
      onEvent<LogOut> {
        workflows.abandonDelegate(state.id)
        EnterState(Authenticating())
      }

      onSuccess(workflows.nextDelegateReaction(state)) {
        when (it) {
          is EnterState -> EnterState(state.copy(delegateState = it.state))
          is FinishWith -> EnterState(RunningGame())
        }
      }
    }
  }
}
