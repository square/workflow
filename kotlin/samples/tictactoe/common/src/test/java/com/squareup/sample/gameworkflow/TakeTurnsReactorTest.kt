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
package com.squareup.sample.gameworkflow

import com.squareup.sample.gameworkflow.Ending.Draw
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.Player.O
import com.squareup.sample.gameworkflow.Player.X
import com.squareup.sample.gameworkflow.TakeTurnsEvent.Quit
import com.squareup.sample.gameworkflow.TakeTurnsEvent.TakeSquare
import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.rx2.result
import com.squareup.workflow.legacy.rx2.state
import com.squareup.workflow.legacy.test.assertFinish
import com.squareup.workflow.legacy.test.assertTransition
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

class TakeTurnsReactorTest {
  @Test fun `read write completed game`() {
    val turn = Turn("biz", "baz")
    val before = CompletedGame(Quitted, turn)
    val out = before.toSnapshot()
    val after = CompletedGame.fromSnapshot(out.bytes)
    assertThat(after).isEqualTo(before)
  }

  @Test fun `starts game with given names`() {
    val workflow = TakeTurnsReactor()
        .launch(Turn("higgledy", "piggledy"), WorkflowPool())
    val tester = workflow.state.test()
    val turn = tester.values()[0]

    assertThat(turn.players[X]).isEqualTo("higgledy")
    assertThat(turn.players[O]).isEqualTo("piggledy")
  }

  @Test fun `x wins`() {
    val workflow = TakeTurnsReactor()
        .launch(Turn("higgledy", "piggledy"), WorkflowPool())
    @Suppress("UNCHECKED_CAST")
    val tester = workflow.result.test() as TestObserver<CompletedGame>

    workflow.sendEvent(TakeSquare(0, 0)) // x
    tester.assertNotTerminated()
    workflow.sendEvent(TakeSquare(1, 0)) // o
    tester.assertNotTerminated()
    workflow.sendEvent(TakeSquare(0, 1)) // x
    tester.assertNotTerminated()
    workflow.sendEvent(TakeSquare(1, 1)) // o
    tester.assertNotTerminated()
    workflow.sendEvent(TakeSquare(0, 2)) // x

    tester.assertTerminated()

    val expectedLastTurn = Turn("higgledy", "piggledy")
        .copy(
            board = listOf(
                listOf(X, X, X),
                listOf(O, O, null),
                listOf(null, null, null)
            )
        )
    tester.assertValue(
        CompletedGame(Victory, expectedLastTurn)
    )
  }

  @Test fun draw() {
    val penultimateTurn = Turn("higgledy", "piggledy")
        .copy(
            playing = X,
            board = listOf(
                listOf(X, O, X),
                listOf(O, O, X),
                listOf(X, null, O)
            )
        )

    val expectedLastTurn = penultimateTurn.copy(
        board = listOf(
            listOf(X, O, X),
            listOf(O, O, X),
            listOf(X, X, O)
        )
    )

    val workflow = TakeTurnsReactor()
        .launch(penultimateTurn, WorkflowPool())
    @Suppress("UNCHECKED_CAST")
    val tester = workflow.result.test() as TestObserver<CompletedGame>
    workflow.sendEvent(TakeSquare(2, 1))

    tester.assertTerminated()
    tester.assertValue(CompletedGame(Draw, expectedLastTurn))
  }

  @Test fun `picking an already picked square enters same state`() {
    val state = Turn(
        players = mapOf(X to "higgledy", O to "piggledy"),
        board = listOf(
            listOf(X, X, X),
            listOf(O, O, null),
            listOf(null, null, null)
        )
    )
    TakeTurnsReactor()
        .assertTransition(
            fromState = state,
            event = TakeSquare(0, 0),
            toState = state
        )
  }

  @Test fun `quit finishes game`() {
    val turn = Turn(
        players = mapOf(X to "higgledy", O to "piggledy"),
        board = listOf(
            listOf(X, X, X),
            listOf(O, O, null),
            listOf(null, null, null)
        )
    )
    TakeTurnsReactor()
        .assertFinish(
            fromState = turn,
            event = Quit,
            output = CompletedGame(Quitted, turn)
        )
  }
}
