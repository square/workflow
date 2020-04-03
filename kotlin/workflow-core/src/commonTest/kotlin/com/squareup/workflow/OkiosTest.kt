package com.squareup.workflow

import okio.Buffer
import okio.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class OkiosTest {

  private val buffer = Buffer()

  @Test fun `write and read nullable skips block when value is null`() {
    val value: String? = null

    buffer.writeNullable(value) {
      fail("Expected write block not to run")
    }
    val result = buffer.readNullable {
      fail("Expected read block not to run")
    }

    assertNull(result)
  }

  @Test fun `write and read nullable run block when value not null`() {
    val value: String? = "foo"
    var writeBlockCalls = 0
    var readBlockCalls = 0

    buffer.writeNullable(value) {
      writeBlockCalls++
    }
    val result = buffer.readNullable {
      readBlockCalls++
    }

    assertNotNull(result)
    assertEquals(1, writeBlockCalls)
    assertEquals(1, readBlockCalls)
  }

  @Test fun `write and read boolean`() {
    buffer.writeBooleanAsInt(true)
    assertEquals(true, buffer.readBooleanFromInt())
  }

  @Test fun `write and read float`() {
    buffer.writeFloat(4.2f)
    assertEquals(4.2f, buffer.readFloat())
  }

  @Test fun `write and read utf8 with length`() {
    buffer.writeUtf8WithLength("foo")
    assertEquals("foo", buffer.readUtf8WithLength())
  }

  @Test fun `write and read empty utf8 with length`() {
    buffer.writeUtf8WithLength("")
    assertEquals("", buffer.readUtf8WithLength())
  }

  @Test fun `write and read ByteString with length`() {
    val bytes = ByteString.of(0xb, 0xe, 0xe, 0xf)
    buffer.writeByteStringWithLength(bytes)
    assertEquals(bytes, buffer.readByteStringWithLength())
  }

  @Test fun `write and read empty ByteString with length`() {
    buffer.writeByteStringWithLength(ByteString.EMPTY)
    assertEquals(ByteString.EMPTY, buffer.readByteStringWithLength())
  }

  @Test fun `write and read empty list`() {
    buffer.writeList(emptyList<String>()) {
      fail("Expected write block to not be called")
    }
    val result = buffer.readList {
      fail("Expected read block to not be called")
    }
    assertTrue(result.isEmpty())
  }

  @Test fun `write and read single element list`() {
    buffer.writeList(listOf("foo")) {
      writeUtf8WithLength(it)
    }
    val result = buffer.readList {
      readUtf8WithLength()
    }

    assertEquals(listOf("foo"), result)
  }

  @Test fun `write and read multiple element list`() {
    buffer.writeList(listOf("foo", "bar")) {
      writeUtf8WithLength(it)
    }
    val result = buffer.readList {
      readUtf8WithLength()
    }

    assertEquals(listOf("foo", "bar"), result)
  }
}
