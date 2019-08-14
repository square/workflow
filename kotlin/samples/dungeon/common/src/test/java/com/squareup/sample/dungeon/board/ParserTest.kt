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
package com.squareup.sample.dungeon.board

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.Test

class ParserTest {

  @Test fun square() {
    val board = """
      🌳🌳
      🌳🌳
    """.trimIndent()
        .parseBoard()

    assertThat(board.width).isEqualTo(2)
    assertThat(board.height).isEqualTo(2)
  }

  @Test fun `pads width centered when tall odd`() {
    val board = """
      🌳🌳
      🌳🌳
      🌳🌳
    """.trimIndent()
        .parseBoard()

    assertThat(board.width).isEqualTo(3)
    assertThat(board.height).isEqualTo(3)
    assertThat(board).isEqualTo(
        """
          |🌳🌳 
          |🌳🌳 
          |🌳🌳 
    """.trimMargin().parseBoard()
    )
  }

  @Test fun `pads width centered when tall even`() {
    val board = """
      🌳🌳
      🌳🌳
      🌳🌳
      🌳🌳
    """.trimIndent()
        .parseBoard()

    assertThat(board.width).isEqualTo(4)
    assertThat(board.height).isEqualTo(4)
    assertThat(board).isEqualTo(
        """
          | 🌳🌳 
          | 🌳🌳 
          | 🌳🌳 
          | 🌳🌳 
    """.trimMargin().parseBoard()
    )
  }

  @Test fun `pads height centered when wide odd`() {
    val board = """
      🌳🌳🌳
      🌳🌳🌳
    """.trimIndent()
        .parseBoard()

    assertThat(board.width).isEqualTo(3)
    assertThat(board.height).isEqualTo(3)
    assertThat(board).isEqualTo(
        """
          |🌳🌳🌳
          |🌳🌳🌳
          |   
    """.trimMargin().parseBoard()
    )
  }

  @Test fun `pads height centered when wide even`() {
    val board = """
      🌳🌳🌳🌳
      🌳🌳🌳🌳
    """.trimIndent()
        .parseBoard()

    assertThat(board.width).isEqualTo(4)
    assertThat(board.height).isEqualTo(4)
    assertThat(board).isEqualTo(
        """
          |    
          |🌳🌳🌳🌳
          |🌳🌳🌳🌳
          |    
    """.trimMargin().parseBoard()
    )
  }

  private fun String.parseBoard(): Board = runBlocking {
    Buffer().writeUtf8(this@parseBoard)
        .parseBoard()
  }
}
