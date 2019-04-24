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

import com.squareup.sample.gameworkflow.Ending.Quitted
import com.squareup.sample.gameworkflow.Ending.Victory
import com.squareup.sample.gameworkflow.GamePlayScreen.Event.TakeSquare
import com.squareup.sample.gameworkflow.Player.O
import com.squareup.sample.gameworkflow.Player.X
import com.squareup.workflow.testing.WorkflowTester
import com.squareup.workflow.testing.testFromStart
import org.assertj.core.api.Assertions
import org.junit.Test

class TakeTurnsWorkflowTest {
  @Test fun readWriteCompletedGame() {
    val turn = Turn()
    val before = CompletedGame(Quitted, turn)
    val out = before.toSnapshot()
    val after = CompletedGame.fromSnapshot(out.bytes)
    Assertions.assertThat(after)
        .isEqualTo(before)
  }

  @Test fun startsGameWithGivenNames() {
    RealTakeTurnsWorkflow().testFromStart(PlayerInfo("higgledy", "piggledy")) {
      val (x, o) = awaitNextRendering().playerInfo

      Assertions.assertThat(x)
          .isEqualTo("higgledy")
      Assertions.assertThat(o)
          .isEqualTo("piggledy")
      println("done")
    }
  }

  @Test fun xWins() {
    RealTakeTurnsWorkflow().testFromStart(PlayerInfo("higgledy", "piggledy")) {
      takeSquare(TakeSquare(0, 0))
      takeSquare(TakeSquare(1, 0))
      takeSquare(TakeSquare(0, 1))
      takeSquare(TakeSquare(1, 1))
      takeSquare(TakeSquare(0, 2))

      val expectedLastTurn = Turn(
          board = listOf(
              listOf(X, X, X),
              listOf(O, O, null),
              listOf(null, null, null)
          )
      )

      val result = awaitNextOutput()
      Assertions.assertThat(result)
          .isEqualTo(CompletedGame(Victory, expectedLastTurn))
    }
  }
}

private suspend fun WorkflowTester<*, *, GamePlayScreen>.takeSquare(event: TakeSquare) {
  awaitNextRendering().onEvent(event)
}
