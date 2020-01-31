package com.squareup.sample.mainworkflow

import com.google.common.truth.Truth.assertThat
import com.squareup.sample.authworkflow.AuthResult.Authorized
import com.squareup.sample.authworkflow.AuthWorkflow
import com.squareup.sample.container.panel.PanelContainerScreen
import com.squareup.sample.gameworkflow.GamePlayScreen
import com.squareup.sample.gameworkflow.RunGameScreen
import com.squareup.sample.gameworkflow.RunGameWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
import com.squareup.workflow.rendering
import com.squareup.workflow.stateless
import com.squareup.workflow.testing.testFromStart
import com.squareup.workflow.ui.backstack.BackStackScreen
import org.junit.Test

/**
 * Demonstrates unit testing of a composite workflow. Note how we
 * pass in fakes for the nested workflows.
 */
class MainWorkflowTest {
  @Test fun `starts in auth over empty game`() {
    MainWorkflow(authWorkflow(), runGameWorkflow()).testFromStart {
      awaitNextRendering()
          .let { screen ->
            assertThat(screen.panels).hasSize(1)
            assertThat(screen.panels[0]).isEqualTo(DEFAULT_AUTH)

            // This GamePlayScreen() is emitted by MainWorkflow itself.
            assertThat(screen.body).isEqualTo(GamePlayScreen())
          }
    }
  }

  @Test fun `starts game on auth`() {
    val authWorkflow: AuthWorkflow = Workflow.stateless {
      runningWorker(Worker.from { Unit }) {
        action { setOutput(Authorized("auth")) }
      }
      authScreen()
    }

    MainWorkflow(authWorkflow, runGameWorkflow()).testFromStart {
      awaitNextRendering()
          .let { screen ->
            assertThat(screen.panels).isEmpty()
            assertThat(screen.body).isEqualTo(DEFAULT_RUN_GAME)
          }
    }
  }

  private fun runGameScreen(
    body: String = DEFAULT_RUN_GAME
  ) = RunGameScreen(PanelContainerScreen(body))

  private fun authScreen(wrapped: String = DEFAULT_AUTH) =
    BackStackScreen<Any>(wrapped)

  private val RunGameScreen.panels: List<Any> get() = beneathModals.modals.map { it.top }
  private val RunGameScreen.body: Any get() = beneathModals.beneathModals.wrapped

  private fun authWorkflow(
    screen: String = DEFAULT_AUTH
  ): AuthWorkflow = Workflow.rendering(authScreen(screen))

  private fun runGameWorkflow(
    body: String = DEFAULT_RUN_GAME
  ): RunGameWorkflow = Workflow.rendering(runGameScreen(body))

  private companion object {
    const val DEFAULT_AUTH = "DefaultAuthScreen"
    const val DEFAULT_RUN_GAME = "DefaultRunGameBody"
  }
}
