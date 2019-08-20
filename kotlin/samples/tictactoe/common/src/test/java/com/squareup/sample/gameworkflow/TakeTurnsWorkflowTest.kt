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

import com.google.common.truth.Truth.assertThat
import com.squareup.sample.gameworkflow.Ending.Draw
import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.Player.O
import com.squareup.sample.gameworkflow.Player.X
import com.squareup.workflow.testing.WorkflowTester
import com.squareup.workflow.testing.testFromStart
import org.junit.Test

class TakeTurnsWorkflowTest {
  @Test fun readWriteCompletedGame() {
    val turn = Turn()
    val before = CompletedGame(Quitted, turn)
    val out = before.toSnapshot()
    val after = CompletedGame.fromSnapshot(out.bytes)
    assertThat(after).isEqualTo(before)
  }

  @Test fun startsGameWithGivenNames() {
    RealTakeTurnsWorkflow().testFromStart(
        TakeTurnsProps.newGame(PlayerInfo("higgledy", "piggledy"))
    ) {
      val (x, o) = awaitNextRendering().playerInfo

      assertThat(x)
          .isEqualTo("higgledy")
      assertThat(o)
          .isEqualTo("piggledy")
    }
  }

  @Test fun xWins() {
    RealTakeTurnsWorkflow().testFromStart(
        TakeTurnsProps.newGame(PlayerInfo("higgledy", "piggledy"))
    ) {
      takeSquare(0, 0)
      takeSquare(1, 0)
      takeSquare(0, 1)
      takeSquare(1, 1)
      takeSquare(0, 2)

      val expectedLastTurn = Turn(
          board = listOf(
              listOf(X, X, X),
              listOf(O, O, null),
              listOf(null, null, null)
          )
      )

      val result = awaitNextOutput()
      assertThat(result).isEqualTo(CompletedGame(Victory, expectedLastTurn))
    }
  }

  @Test fun draw() {
    RealTakeTurnsWorkflow().testFromStart(
        TakeTurnsProps.newGame(PlayerInfo("higgledy", "piggledy"))
    ) {
      takeSquare(0, 0) // X - -
      takeSquare(0, 1) // X O -
      takeSquare(0, 2) // X O X

      takeSquare(1, 2) // - - O
      takeSquare(1, 0) // X - O
      takeSquare(1, 1) // X O O

      takeSquare(2, 2) // - - X
      takeSquare(2, 0) // O - X
      takeSquare(2, 1) // O X X

      val expectedLastTurn = Turn(
          board = listOf(
              listOf(X, O, X),
              listOf(X, O, O),
              listOf(O, X, X)
          )
      )

      val result = awaitNextOutput()
      assertThat(result).isEqualTo(CompletedGame(Draw, expectedLastTurn))
    }
  }

  @Test fun quiteAndResume() {
    var output: CompletedGame? = null

    RealTakeTurnsWorkflow().testFromStart(
        TakeTurnsProps.newGame(PlayerInfo("higgledy", "piggledy"))
    ) {
      awaitNextRendering().onQuit()
      output = awaitNextOutput()
    }

    assertThat(output!!.ending).isSameInstanceAs(Quitted)

    RealTakeTurnsWorkflow().testFromStart(
        TakeTurnsProps.resumeGame(
            PlayerInfo("higgledy", "piggledy"),
            output!!.lastTurn
        )
    ) {
      assertThat(awaitNextRendering().gameState).isEqualTo(output!!.lastTurn)
    }
  }
}

private fun WorkflowTester<*, *, GamePlayScreen>.takeSquare(row: Int, col: Int) {
  awaitNextRendering().onClick(row, col)
}
