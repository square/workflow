package com.squareup.sample.mainworkflow

import com.squareup.sample.authworkflow.AuthWorkflow
import com.squareup.sample.gameworkflow.GamePlayScreen
import com.squareup.sample.gameworkflow.RunGameScreen
import com.squareup.sample.gameworkflow.RunGameWorkflow
import com.squareup.sample.panel.PanelContainerScreen
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.onWorkerOutput
import com.squareup.workflow.rendering
import com.squareup.workflow.stateless
import com.squareup.workflow.testing.testFromStart
import com.squareup.workflow.ui.BackStackScreen
import org.assertj.core.api.Java6Assertions.assertThat
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
    val authWorkflow: AuthWorkflow = Workflow.stateless { context ->
      context.onWorkerOutput(Worker.from { Unit }) { emitOutput("auth") }
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
  ) = RunGameScreen(PanelContainerScreen<String, String>(body))

  private fun authScreen(wrapped: String = DEFAULT_AUTH) =
    BackStackScreen(wrapped)

  private val RunGameScreen.panels: List<Any> get() = baseScreen.modals.map { it.wrapped }
  private val RunGameScreen.body: Any get() = baseScreen.baseScreen

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
