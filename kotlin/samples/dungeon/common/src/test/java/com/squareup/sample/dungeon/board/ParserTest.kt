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
import okio.Buffer
import org.junit.Test
import kotlin.test.assertFailsWith

class ParserTest {

  @Test fun `parseBoardMetadata throws when empty`() {
    val board = ""

    val error = assertFailsWith<IllegalArgumentException> {
      board.toBufferedSource()
          .parseBoardMetadata()
    }
    assertThat(error).hasMessageThat()
        .contains("No board metadata found in stream")
  }

  @Test fun `parseBoardMetadata throws when missing`() {
    val board = """
      🌳🌳
      🌳🌳
    """.trimIndent()

    val error = assertFailsWith<IllegalArgumentException> {
      board.toBufferedSource()
          .parseBoardMetadata()
    }
    assertThat(error).hasMessageThat()
        .contains("No board metadata found in stream")
  }

  @Test fun `parseBoardMetadata throws when header not closed`() {
    val board = """
      ---
      🌳🌳
      🌳🌳
    """.trimIndent()

    val error = assertFailsWith<IllegalArgumentException> {
      board.toBufferedSource()
          .parseBoardMetadata()
    }
    assertThat(error).hasMessageThat()
        .isEqualTo("Expected --- but found EOF.")
  }

  @Test fun `parseBoardMetadata throws when document is empty`() {
    val board = """
      ---
      ---
    """.trimIndent()

    val error = assertFailsWith<IllegalArgumentException> {
      board.toBufferedSource()
          .parseBoardMetadata()
    }
    assertThat(error).hasMessageThat()
        .isEqualTo("Error parsing board metadata.")
    assertThat(error).hasCauseThat()
        .hasMessageThat()
        .isEqualTo("The YAML document is empty.")
  }

  @Test fun `parseBoardMetadata throws when document has unknown fields`() {
    val board = """
      ---
      foobarbaz: false
      ---
    """.trimIndent()

    val error = assertFailsWith<IllegalArgumentException> {
      board.toBufferedSource()
          .parseBoardMetadata()
    }
    assertThat(error).hasMessageThat()
        .isEqualTo("Error parsing board metadata.")
    assertThat(error).hasCauseThat()
        .hasMessageThat()
        .contains("foobarbaz")
  }

  @Test fun `parseBoardMetadata parses valid header`() {
    val board = """
      ---
      name: Såm ✅
      ---
    """.trimIndent()

    val metadata = board.toBufferedSource()
        .parseBoardMetadata()
    assertThat(metadata).isEqualTo(BoardMetadata(name = "Såm ✅"))
  }

  @Test fun `parse parses metadata and board`() {
    val board = """
      ---
      name: Foo
      ---
      🌳🌳
      🌳🌳
    """.trimIndent()
        // Don't call parseBoard on the string directly, since that fakes the metadata.
        .toBufferedSource()
        .parseBoard()

    assertThat(board.cells).isNotEmpty()
    assertThat(board.metadata).isEqualTo(BoardMetadata(name = "Foo"))
  }

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

  @Suppress("BlockingMethodInNonBlockingContext")
  private fun String.parseBoard(): Board =
    toBufferedSource().parseBoard(metadata = BoardMetadata("test"))

  private fun String.toBufferedSource() = Buffer().writeUtf8(this)
}
