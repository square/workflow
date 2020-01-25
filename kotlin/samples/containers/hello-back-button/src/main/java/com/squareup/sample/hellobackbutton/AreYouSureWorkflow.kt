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
package com.squareup.sample.hellobackbutton

import com.squareup.sample.container.BackButtonScreen
import com.squareup.sample.hellobackbutton.AreYouSureWorkflow.Finished
import com.squareup.sample.hellobackbutton.AreYouSureWorkflow.State
import com.squareup.sample.hellobackbutton.AreYouSureWorkflow.State.Quitting
import com.squareup.sample.hellobackbutton.AreYouSureWorkflow.State.Running
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.action
import com.squareup.workflow.ui.modal.AlertContainerScreen
import com.squareup.workflow.ui.modal.AlertScreen
import com.squareup.workflow.ui.modal.AlertScreen.Button.NEGATIVE
import com.squareup.workflow.ui.modal.AlertScreen.Button.POSITIVE
import com.squareup.workflow.ui.modal.AlertScreen.Event.ButtonClicked
import com.squareup.workflow.ui.modal.AlertScreen.Event.Canceled

/**
 * Wraps [HelloBackButtonWorkflow] to (sometimes) pop a confirmation dialog when the back
 * button is pressed.
 */
object AreYouSureWorkflow : StatefulWorkflow<Unit, State, Finished, AlertContainerScreen<*>>() {
  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = Running

  enum class State {
    Running,
    Quitting
  }

  object Finished

  override fun render(
    props: Unit,
    state: State,
    context: RenderContext<State, Finished>
  ): AlertContainerScreen<*> {
    val ableBakerCharlie = context.renderChild(HelloBackButtonWorkflow, Unit) { noAction() }

    return when (state) {
      Running -> {
        AlertContainerScreen(
            BackButtonScreen(ableBakerCharlie) {
              // While we always provide a back button handler, by default the view code
              // associated with BackButtonScreen ignores ours if the view created for the
              // wrapped rendering sets a handler of its own. (Set BackButtonScreen.override
              // to change this precedence.)
              context.actionSink.send(maybeQuit)
            }
        )
      }
      Quitting -> {
        val dialog = AlertScreen(
            buttons = mapOf(
                POSITIVE to "I'm Positive",
                NEGATIVE to "Negatory"
            ),
            message = "Are you sure you want to do this thing?",
            onEvent = { alertEvent ->
              context.actionSink.send(
                  when (alertEvent) {
                    is ButtonClicked -> when (alertEvent.button) {
                      POSITIVE -> confirmQuit
                      else -> cancelQuit
                    }
                    Canceled -> cancelQuit
                  }
              )
            }
        )

        AlertContainerScreen(ableBakerCharlie, dialog)
      }
    }
  }

  override fun snapshotState(state: State) = Snapshot.EMPTY

  private val maybeQuit = action { nextState = Quitting }
  private val confirmQuit = action { setOutput(Finished) }
  private val cancelQuit = action { nextState = Running }
}
