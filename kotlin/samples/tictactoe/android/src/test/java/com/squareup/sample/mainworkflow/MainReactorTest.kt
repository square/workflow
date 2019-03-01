package com.squareup.sample.mainworkflow

import com.squareup.sample.mainworkflow.MainState.Authenticating
import com.squareup.sample.mainworkflow.MainState.RunningGame
import com.squareup.sample.authworkflow.AuthEvent
import com.squareup.sample.authworkflow.AuthLauncher
import com.squareup.sample.authworkflow.AuthState
import com.squareup.sample.gameworkflow.RunGameEvent
import com.squareup.sample.gameworkflow.RunGameLauncher
import com.squareup.sample.gameworkflow.RunGameResult
import com.squareup.sample.gameworkflow.RunGameState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.rx2.state
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

/**
 * Demonstrates writing unit test of a composite Reactor. Note how
 * we pass in fakes for the reactors nested by [MainReactor].
 */
class MainReactorTest {
  private val pool = WorkflowPool()

  private val runGame = object : RunGameLauncher,
      TestLauncher<RunGameState, RunGameEvent, RunGameResult>() {}

  private val auth = object : AuthLauncher,
      TestLauncher<AuthState, AuthEvent, String>() {}

  private val workflow = MainReactor(runGame, auth)
      .launch(MainState.startingState(), pool)

  @Test fun `starts in auth`() {
    val tester = workflow.state.test()
    tester.assertValueCount(1)
    assertThat(tester.values()[0]).isInstanceOf(Authenticating::class.java)
  }

  @Test fun `starts game on auth`() {
    auth.reactions.onNext(FinishWith("auth"))

    val tester = workflow.state.test()
    tester.assertValueCount(1)
    assertThat(tester.values()[0]).isInstanceOf(RunningGame::class.java)
  }

  @Test fun `can log out during game`() {
    val tester = workflow.state.test()

    auth.reactions.onNext(FinishWith("auth"))
    workflow.sendEvent(LogOut)

    assertThat(tester.values()[0]).isInstanceOf(Authenticating::class.java)
    assertThat(tester.values()[1]).isInstanceOf(RunningGame::class.java)
    assertThat(tester.values()[2]).isInstanceOf(Authenticating::class.java)

    tester.assertValueCount(3)
  }
}
