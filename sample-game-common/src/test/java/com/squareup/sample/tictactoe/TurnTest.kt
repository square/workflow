package com.squareup.sample.tictactoe

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

class TurnTest {
  @Test fun readWriteTurn() {
    val before = Turn("able", "baker")
    val out = before.toSnapshot()
    val after = Turn.fromSnapshot(out.bytes)
    assertThat(after).isEqualTo(before)
  }
}
