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
package com.squareup.workflow.internal

import com.squareup.workflow.internal.InlineLinkedList.InlineListNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InlineLinkedListTest {

  @Test fun `forEach empty list`() {
    val list = InlineLinkedList<StringElement>()
    var count = 0
    list.forEach { count++ }
    assertEquals(0, count)
  }

  @Test fun `plusAssign on empty list`() {
    val list = InlineLinkedList<StringElement>()

    list += StringElement("foo")

    assertEquals(listOf("foo"), list.toList())
  }

  @Test fun `removeFirst on empty list`() {
    val list = InlineLinkedList<StringElement>()

    list.removeFirst { true }

    assertEquals(emptyList(), list.toList())
  }

  @Test fun `removeFirst on single-item list`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")

    assertEquals("foo", list.removeFirst { it.value == "foo" }?.value)

    assertEquals(emptyList(), list.toList())
  }

  @Test fun `removeFirst head on list with 2 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")

    assertEquals("foo", list.removeFirst { it.value == "foo" }?.value)

    assertEquals(listOf("bar"), list.toList())
  }

  @Test fun `removeFirst tail on list with 2 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")

    assertEquals("bar", list.removeFirst { it.value == "bar" }?.value)

    assertEquals(listOf("foo"), list.toList())
  }

  @Test fun `removeFirst head on list with 3 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list += StringElement("baz")

    assertEquals("foo", list.removeFirst { it.value == "foo" }?.value)

    assertEquals(listOf("bar", "baz"), list.toList())
  }

  @Test fun `removeFirst middle on list with 3 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list += StringElement("baz")

    assertEquals("bar", list.removeFirst { it.value == "bar" }?.value)

    assertEquals(listOf("foo", "baz"), list.toList())
  }

  @Test fun `removeFirst tail on list with 3 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list += StringElement("baz")

    assertEquals("baz", list.removeFirst { it.value == "baz" }?.value)

    assertEquals(listOf("foo", "bar"), list.toList())
  }

  @Test fun `removeFirst when multiple matches`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("foo")
    list += StringElement("bar")

    assertEquals("foo", list.removeFirst { it.value == "foo" }?.value)

    assertEquals(listOf("foo", "bar"), list.toList())
  }

  @Test fun `removeFirst when no matches`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")

    assertNull(list.removeFirst { it.value == "baz" })

    assertEquals(listOf("foo", "bar"), list.toList())
  }

  @Test fun `plusAssign on non-empty list`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")

    list += StringElement("bar")

    assertEquals(listOf("foo", "bar"), list.toList())
  }

  @Test fun `plusAssign after remove head with 2 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list.removeFirst { it.value == "foo" }

    list += StringElement("buzz")

    assertEquals(listOf("bar", "buzz"), list.toList())
  }

  @Test fun `plusAssign after remove tail with 2 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list.removeFirst { it.value == "bar" }

    list += StringElement("buzz")

    assertEquals(listOf("foo", "buzz"), list.toList())
  }

  @Test fun `plusAssign after remove head with 3 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list += StringElement("baz")
    list.removeFirst { it.value == "foo" }

    list += StringElement("buzz")

    assertEquals(listOf("bar", "baz", "buzz"), list.toList())
  }

  @Test fun `plusAssign after remove middle with 3 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list += StringElement("baz")
    list.removeFirst { it.value == "bar" }

    list += StringElement("buzz")

    assertEquals(listOf("foo", "baz", "buzz"), list.toList())
  }

  @Test fun `plusAssign after remove tail with 3 items`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list += StringElement("baz")
    list.removeFirst { it.value == "baz" }

    list += StringElement("buzz")

    assertEquals(listOf("foo", "bar", "buzz"), list.toList())
  }

  @Test fun `clear empty list`() {
    val list = InlineLinkedList<StringElement>()
    list.clear()
    assertEquals(emptyList(), list.toList())
  }

  @Test fun `clear single-item list`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list.clear()
    assertEquals(emptyList(), list.toList())
  }

  @Test fun `clear multi-item list`() {
    val list = InlineLinkedList<StringElement>()
    list += StringElement("foo")
    list += StringElement("bar")
    list.clear()
    assertEquals(emptyList(), list.toList())
  }

  private fun InlineLinkedList<StringElement>.toList(): List<String> {
    val items = mutableListOf<String>()
    forEach { items += it.value }
    return items
  }
}

private class StringElement(
  val value: String
) : InlineListNode<StringElement> {
  override var nextListNode: StringElement? = null
}
