/*
 * Copyright 2020 Square Inc.
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
package com.squareup.workflow.ui.compose

import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.test.assertIsVisible
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
class ComposableDecoratorTest {

  @Rule @JvmField val composeTestRule = createComposeRule()

  @Test fun assertingComposableDecorator_works() {
    val wrapped = ComposableDecorator { children ->
      Column {
        Text("Parent")
        children()
      }
    }
    val decorator = AssertingComposableDecorator(wrapped)

    composeTestRule.setContent {
      decorator.decorate {
        Text("Child")
      }
    }

    composeTestRule.runOnIdleCompose {
      findByText("Parent")
    }
        .assertIsVisible()
    findByText("Child").assertIsVisible()
  }

  @Test fun assertingComposableDecorator_throws_whenChildrenNotInvoked() {
    val wrapped = ComposableDecorator { }
    val decorator = AssertingComposableDecorator(wrapped)

    val error = assertFailsWith<IllegalStateException> {
      composeTestRule.setContent {
        decorator.decorate {}
      }
    }

    assertEquals(
        "Expected ComposableDecorator to invoke children exactly once, but was invoked 0 times.",
        error.message
    )
  }

  @Test fun assertingComposableDecorator_throws_whenChildrenInvokedMultipleTimes() {
    val wrapped = ComposableDecorator { children ->
      children()
      children()
    }
    val decorator = AssertingComposableDecorator(wrapped)

    val error = assertFailsWith<IllegalStateException> {
      composeTestRule.setContent {
        decorator.decorate {
          Text("Hello")
        }
      }
    }

    assertEquals(
        "Expected ComposableDecorator to invoke children exactly once, but was invoked 2 times.",
        error.message
    )
  }
}
