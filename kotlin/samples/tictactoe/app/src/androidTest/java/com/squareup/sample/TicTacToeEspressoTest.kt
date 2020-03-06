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
package com.squareup.sample

import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.sample.gameworkflow.GamePlayScreen
import com.squareup.sample.gameworkflow.Player
import com.squareup.sample.gameworkflow.symbol
import com.squareup.sample.mainactivity.MainActivity
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.environment
import com.squareup.workflow.ui.getRendering
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class TicTacToeEspressoTest {

  private lateinit var scenario: ActivityScenario<MainActivity>

  @Before
  fun setUp() {
    scenario = launch(MainActivity::class.java)
        .apply {
          onActivity { activity ->
            IdlingRegistry.getInstance()
                .register(activity.idlingResource)
            activity.requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
          }
        }
  }

  @After
  fun tearDown() {
    scenario.onActivity { activity ->
      IdlingRegistry.getInstance()
          .unregister(activity.idlingResource)
    }
  }

  @Test fun showRenderingTagStaysFresh() {
    // Start a game so that there's something interesting in the Activity window.
    // (Prior screens are all in a dialog window.)

    onView(withId(R.id.login_email)).type("foo@bar")
    onView(withId(R.id.login_password)).type("password")
    onView(withId(R.id.login_button)).perform(click())

    onView(withId(R.id.start_game)).perform(click())

    val environment = AtomicReference<ViewEnvironment>()

    // Why should I learn how to write a matcher when I can just grab the activity
    // and work with it directly?
    scenario.onActivity { activity ->
      val button = activity.findViewById<View>(R.id.game_play_board)
      val parent = button.parent as View
      val rendering = parent.getRendering<GamePlayScreen>()!!
      assertThat(rendering.gameState.playing).isSameInstanceAs(Player.X)
      val firstHints = parent.environment
      assertThat(firstHints).isNotNull()
      environment.set(firstHints)

      // Make a move.
      rendering.onClick(0, 0)
    }

    // I'm not an animal, though. Pop back out to the test to check that the update
    // has happened, to make sure the idle check is allowed to do its thing. (Didn't
    // actually seem to be necessary, originally did everything synchronously in the
    // lambda above and it all worked just fine. But that seems like a land mine.)

    onView(withId(R.id.game_play_toolbar))
        .check(matches(hasDescendant(withText("O, place your ${Player.O.symbol}"))))

    // Now that we're confident the views have updated, back to the activity
    // to mess with what should be the updated rendering.
    scenario.onActivity { activity ->
      val button = activity.findViewById<View>(R.id.game_play_board)
      val parent = button.parent as View
      val rendering = parent.getRendering<GamePlayScreen>()!!
      assertThat(rendering.gameState.playing).isSameInstanceAs(Player.O)
      assertThat(parent.environment).isEqualTo(environment.get())
    }
  }

  @Test fun configChangeReflectsWorkflowState() {
    onView(withId(R.id.login_email)).type("bad email")
    onView(withId(R.id.login_button)).perform(click())

    onView(withId(R.id.login_error_message)).check(matches(withText("Invalid address")))
    rotate()
    onView(withId(R.id.login_error_message)).check(matches(withText("Invalid address")))
  }

  @Test fun editTextSurvivesConfigChange() {
    onView(withId(R.id.login_email)).type("foo@bar")
    onView(withId(R.id.login_password)).type("password")
    rotate()
    onView(withId(R.id.login_email)).check(matches(withText("foo@bar")))
    // Don't save fields that shouldn't be.
    onView(withId(R.id.login_password)).check(matches(withText("")))
  }

  @Test fun backStackPopRestoresViewState() {
    // The loading screen is pushed onto the back stack.
    onView(withId(R.id.login_email)).type("foo@bar")
    onView(withId(R.id.login_password)).type("bad password")
    onView(withId(R.id.login_button)).perform(click())

    // Loading ends with an error, and we pop back to login. The
    // email should have been restored from view state.
    onView(withId(R.id.login_email)).check(matches(withText("foo@bar")))
    onView(withId(R.id.login_error_message))
        .check(matches(withText("Unknown email or invalid password")))
  }

  @Test fun dialogSurvivesConfigChange() {
    onView(withId(R.id.login_email)).type("foo@bar")
    onView(withId(R.id.login_password)).type("password")
    onView(withId(R.id.login_button)).perform(click())

    onView(withId(R.id.player_X)).type("Mister X")
    onView(withId(R.id.player_O)).type("Sister O")
    onView(withId(R.id.start_game)).perform(click())

    pressBack()
    onView(withText("Do you really want to concede the game?")).check(matches(isDisplayed()))
    rotate()
    onView(withText("Do you really want to concede the game?")).check(matches(isDisplayed()))
  }

  @Test fun canGoBackInModalView() {
    // Log in and hit the 2fa screen.
    onView(withId(R.id.login_email)).type("foo@2fa")
    onView(withId(R.id.login_password)).type("password")
    onView(withId(R.id.login_button)).perform(click())
    onView(withId(R.id.second_factor)).check(matches(isDisplayed()))

    // Use the back button to go back and see the login screen again.
    pressBack()
    // Make sure edit text was restored from view state cached by the back stack container.
    onView(withId(R.id.login_email)).check(matches(withText("foo@2fa")))
  }

  @Test fun configChangePreservesBackStackViewStateCache() {
    // Log in and hit the 2fa screen.
    onView(withId(R.id.login_email)).type("foo@2fa")
    onView(withId(R.id.login_password)).type("password")
    onView(withId(R.id.login_button)).perform(click())
    onView(withId(R.id.second_factor)).check(matches(isDisplayed()))

    // Rotate and then use the back button to go back and see the login screen again.
    rotate()
    pressBack()
    // Make sure edit text was restored from view state cached by the back stack container.
    onView(withId(R.id.login_email)).check(matches(withText("foo@2fa")))
  }

  private fun ViewInteraction.type(text: String) {
    perform(typeText(text), closeSoftKeyboard())
  }

  private fun rotate() {
    scenario.onActivity {
      it.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
    }
  }
}
