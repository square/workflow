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

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LifetimeTrackerTest {

  private data class TestFactory(
    val key: String,
    val value: String = ""
  )

  private inner class TestDisposable(val name: String) {
    var disposed = false

    init {
      disposableCount++
    }

    fun dispose() {
      if (!disposed) {
        disposableCount--
        disposed = true
      }
    }
  }

  private var disposableCount = 0
  private val tracker = LifetimeTracker<TestFactory, String, TestDisposable>(
      getKey = { it.key },
      start = { TestDisposable(it.key) },
      dispose = { factory, disposable ->
        assertEquals(factory.key, disposable.name)
        disposable.dispose()
      }
  )

  @BeforeTest fun setUp() {
    assertEquals(0, disposableCount)
  }

  @AfterTest fun tearDown() {
    tracker.track(emptyList())
    assertEquals(0, disposableCount)
  }

  @Test fun `ensure starts new factory`() {
    tracker.ensure(TestFactory("foo"))
    assertEquals(1, disposableCount)
    assertEquals("foo", tracker.lifetimes.getByKey("foo").name)

    tracker.ensure(TestFactory("bar"))
    assertEquals(2, disposableCount)
    assertEquals("foo", tracker.lifetimes.getByKey("foo").name)
    assertEquals("bar", tracker.lifetimes.getByKey("bar").name)
  }

  @Test fun `track starts new factories`() {
    tracker.track(listOf(TestFactory("foo")))
    assertEquals(1, disposableCount)
    assertEquals("foo", tracker.lifetimes.getByKey("foo").name)

    tracker.track(listOf(TestFactory("foo"), TestFactory("bar"), TestFactory("baz")))
    assertEquals(3, disposableCount)
    assertEquals("foo", tracker.lifetimes.getByKey("foo").name)
    assertEquals("bar", tracker.lifetimes.getByKey("bar").name)
    assertEquals("baz", tracker.lifetimes.getByKey("baz").name)
  }

  @Test fun `track disposes missing factories`() {
    tracker.track(listOf(TestFactory("foo")))
    assertEquals(1, disposableCount)
    assertEquals(1, tracker.lifetimes.count { (f, _) -> f.key == "foo" })

    tracker.track(listOf(TestFactory("bar")))
    assertEquals(1, disposableCount)
    assertEquals(0, tracker.lifetimes.count { (f, _) -> f.key == "foo" })
  }

  @Test fun `track throws on two duplicate keys`() {
    val error = assertFailsWith<IllegalArgumentException> {
      tracker.track(listOf(TestFactory("dup"), TestFactory("dup")))
    }
    assertTrue("Expected all keys to be unique. Duplicates:" in error.message!!)
    assertTrue("2×dup" in error.message!!)
  }

  @Test fun `track throws on three duplicate keys`() {
    val error = assertFailsWith<IllegalArgumentException> {
      tracker.track(listOf(TestFactory("dup"), TestFactory("dup"), TestFactory("dup")))
    }
    assertTrue("3×dup" in error.message!!)
  }

  @Test fun `track throws on multiple sets of duplicate keys`() {
    val error = assertFailsWith<IllegalArgumentException> {
      tracker.track(
          listOf(TestFactory("dup1"), TestFactory("dup2"), TestFactory("dup1"), TestFactory("dup2"))
      )
    }
    assertTrue("2×dup1" in error.message!!)
    assertTrue("2×dup2" in error.message!!)
  }

  // See https://github.com/square/workflow/issues/261.
  @Test fun `ensure updates factory`() {
    tracker.ensure(TestFactory("foo", "initial value"))
    assertEquals("initial value", tracker.lifetimes.single().first.value)

    tracker.ensure(TestFactory("foo", "updated value"))
    assertEquals("updated value", tracker.lifetimes.single().first.value)
  }

  // See https://github.com/square/workflow/issues/261.
  @Test fun `track updates factory`() {
    tracker.track(listOf(TestFactory("foo", "initial value")))
    assertEquals("initial value", tracker.lifetimes.single().first.value)

    tracker.track(listOf(TestFactory("foo", "updated value")))
    assertEquals("updated value", tracker.lifetimes.single().first.value)
  }

  private fun List<Pair<TestFactory, TestDisposable>>.getByKey(key: String) =
    single { (factory, _) -> factory.key == key }.second
}
