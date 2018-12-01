package com.squareup.sample.tictactoe

import com.squareup.sample.tictactoe.Ending.Quitted
import com.squareup.sample.tictactoe.Ending.Victory
import com.squareup.sample.tictactoe.Player.O
import com.squareup.sample.tictactoe.Player.X
import com.squareup.sample.tictactoe.TakeTurnsEvent.TakeSquare
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.rx2.result
import com.squareup.workflow.rx2.state
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

class TakeTurnsReactorTest {
  @Test fun readWriteCompletedGame() {
    val turn = Turn("biz", "baz")
    val before = CompletedGame(Quitted, turn)
    val out = before.toSnapshot()
    val after = CompletedGame.fromSnapshot(out.bytes)
    assertThat(after).isEqualTo(before)
  }

  @Test fun startsGameWithGivenNames() {
    val workflow = TakeTurnsReactor().launch(Turn("higgledy", "piggledy"), WorkflowPool())
    val tester = workflow.state.test()
    val turn = tester.values()[0]

    assertThat(turn.players[X]).isEqualTo("higgledy")
    assertThat(turn.players[O]).isEqualTo("piggledy")
  }

  @Test fun xWins() {
    val workflow = TakeTurnsReactor().launch(Turn("higgledy", "piggledy"), WorkflowPool())
    val tester = workflow.result.test() as TestObserver<CompletedGame>

    workflow.sendEvent(TakeSquare(0, 0))
    workflow.sendEvent(TakeSquare(1, 0))
    workflow.sendEvent(TakeSquare(0, 1))
    workflow.sendEvent(TakeSquare(1, 1))
    workflow.sendEvent(TakeSquare(0, 2))

    tester.assertTerminated()

    val expectedLastTurn = Turn("higgledy", "piggledy")
        .copy(
            board = listOf(
                listOf(X, X, X),
                listOf(O, O, null),
                listOf(null, null, null)
            )
        )
    tester.assertValue(CompletedGame(Victory, expectedLastTurn))
  }
}
