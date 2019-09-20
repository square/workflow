package com.squareup.sample

import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.sample.mainactivity.MainActivity
import com.squareup.sample.tictactoe.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TicTacToeEspressoTest {

  lateinit var scenario: ActivityScenario<MainActivity>

  @Before
  fun setUp() {
    scenario = launch(MainActivity::class.java)
        .apply {
          onActivity {
            IdlingRegistry.getInstance()
                .register(it.idlingResource)
            it.requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
          }
        }
  }

  @After
  fun tearDown() {
    scenario.onActivity {
      IdlingRegistry.getInstance()
          .unregister(it.idlingResource)
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
    onView(withId(R.id.login_password)).check(matches(withText("password")))
  }

  @Test fun backStackPopRestoresViewState() {
    onView(withId(R.id.login_email)).type("foo@bar")
    onView(withId(R.id.login_password)).type("bad password")
    onView(withId(R.id.login_button)).perform(click())
    onView(withId(R.id.login_error_message))
        .check(matches(withText("Unknown email or invalid password")))
    rotate()
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

  private fun ViewInteraction.type(text: String) {
    perform(typeText(text), closeSoftKeyboard())
  }

  private fun rotate() {
    scenario.onActivity {
      it.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
    }
  }
}
