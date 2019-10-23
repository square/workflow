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
package com.squareup.workflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackStackScreenTest {
  @Test fun `top  is last`() {
    assertThat(BackStackScreen(1, 2, 3, 4).top).isEqualTo(4)
  }

  @Test fun `backstack is all but top`() {
    assertThat(BackStackScreen(1, 2, 3, 4).backStack).isEqualTo(listOf(1, 2, 3))
  }

  @Test fun `get works`() {
    assertThat(BackStackScreen("able", "baker", "charlie")[1]).isEqualTo("baker")
  }

  @Test fun `plus another stack`() {
    assertThat(BackStackScreen(1, 2, 3) + BackStackScreen(8, 9, 0))
        .isEqualTo(BackStackScreen(1, 2, 3, 8, 9, 0))
  }

  @Test fun `unequal by order`() {
    assertThat(BackStackScreen(1, 2, 3)).isNotEqualTo(BackStackScreen(3, 2, 1))
  }

  @Test fun `equal have matching hash`() {
    assertThat(BackStackScreen(1, 2, 3).hashCode()).isEqualTo(BackStackScreen(1, 2, 3).hashCode())
  }

  @Test fun `unequal have mismatching hash`() {
    assertThat(BackStackScreen(1, 2).hashCode()).isNotEqualTo(BackStackScreen(1, 2, 3).hashCode())
  }

  @Test fun `empty back and top`() {
    assertThat(BackStackScreen(
        backStack = emptyList(),
        top = 1
    )).isEqualTo(BackStackScreen(1))
  }

  @Test fun `one item back and top`() {
    assertThat(BackStackScreen(
        backStack = listOf(1),
        top = 2
    )).isEqualTo(BackStackScreen(1, 2))
  }

  @Test fun `multi item back and top`() {
    assertThat(BackStackScreen(
        backStack = listOf(1, 2, 3),
        top = 4
    )).isEqualTo(BackStackScreen(1, 2, 3, 4))
  }
}
