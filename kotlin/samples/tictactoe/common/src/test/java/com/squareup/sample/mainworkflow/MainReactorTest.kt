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
package com.squareup.sample.mainworkflow

/**
 * Demonstrates writing unit test of a composite Reactor. Note how
 * we pass in fakes for the reactors nested by [MainWorkflow].
 */
class MainReactorTest {
  // Commented out pending test friendly API change.

//  private val pool = WorkflowPool()
//
//  private val runGame = object : RunGameWorkflow,
//      TestLauncher<RunGameState, RunGameEvent, RunGameResult>() {}
//
//  private val auth = object : AuthWorkflow,
//      TestLauncher<AuthState, AuthEvent, String>() {}
//
//  private val workflow = MainWorkflow(auth, runGame)
//      .launch(MainState.startingState(), pool)
//
//  @Test fun `starts in auth`() {
//    val tester = workflow.state.test()
//    tester.assertValueCount(1)
//    assertThat(tester.values()[0]).isInstanceOf(Authenticating::class.java)
//  }
//
//  @Test fun `starts game on auth`() {
//    auth.reactions.onNext(FinishWith("auth"))
//
//    val tester = workflow.state.test()
//    tester.assertValueCount(1)
//    assertThat(tester.values()[0]).isInstanceOf(RunningGame::class.java)
//  }
//
//  @Test fun `can log out during game`() {
//    val tester = workflow.state.test()
//
//    auth.reactions.onNext(FinishWith("auth"))
//    workflow.sendEvent(LogOut)
//
//    assertThat(tester.values()[0]).isInstanceOf(Authenticating::class.java)
//    assertThat(tester.values()[1]).isInstanceOf(RunningGame::class.java)
//    assertThat(tester.values()[2]).isInstanceOf(Authenticating::class.java)
//
//    tester.assertValueCount(3)
//  }
}
