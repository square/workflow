package com.squareup.sample.dungeon

import com.google.common.truth.Truth.assertThat
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import org.junit.Test

class MovementTest {

  @Test fun `movement plus`() {
    val start = Movement()
    assertThat(start + RIGHT).isEqualTo(Movement(RIGHT))
  }

  @Test fun `movement minus`() {
    val start = Movement(RIGHT, UP)
    assertThat(start - RIGHT).isEqualTo(Movement(UP))
  }
}
