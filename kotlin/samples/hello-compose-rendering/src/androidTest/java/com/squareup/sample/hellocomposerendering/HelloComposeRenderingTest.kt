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
package com.squareup.sample.hellocomposerendering

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.doClick
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelloComposeRenderingTest {

  // Launches the activity.
  @Rule @JvmField val composeRule = AndroidComposeTestRule<HelloComposeRenderingActivity>()

  @Test fun togglesBetweenStates() {
    findByText("Hello")
        .assertIsDisplayed()
        .doClick()
    findByText("Goodbye")
        .assertIsDisplayed()
        .doClick()
    findByText("Hello")
        .assertIsDisplayed()
  }
}
