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

import com.squareup.sample.NavigationState.GO_FORWARD
import com.squareup.sample.NavigationState.RUNNING
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.parse
import com.squareup.workflow.readEnumByOrdinal
import com.squareup.workflow.writeEnumByOrdinal
import kotlin.reflect.jvm.jvmName

/**
 * A [Workflow] that can be rendered by a [ScreenContext].
 * Navigates by emitting [NavigationOutput]s.
 */
typealias SimpleScreenWorkflow<I, O, R, D> = Workflow<ScreenInput<I>, NavigationOutput<O>, Pair<R, D>>

/**
 * Signals emitted by a [SimpleScreenWorkflow] to control navigation.
 */
sealed class NavigationOutput<out O : Any> : WorkflowAction<NavigationState, O> {

  data class Output<O : Any>(val output: O) : NavigationOutput<O>() {
    override fun invoke(state: NavigationState): Pair<NavigationState, O?> {
      return Pair(state, output)
    }
  }

  data class GoForward<O : Any>(val emittingOutput: O? = null) : NavigationOutput<O>() {
    override fun invoke(state: NavigationState): Pair<NavigationState, O?> {
      return Pair(GO_FORWARD, emittingOutput)
    }
  }
}

/**
 * Render a child [SimpleScreenWorkflow] and return its rendering.
 *
 * @see RenderContext.renderChild
 */
fun <S, O : Any, R, D : Any, CI, CO : Any> ScreenContext<S, O, D>.renderChild(
  workflow: SimpleScreenWorkflow<CI, CO, R, D>,
  input: CI,
  key: String = "",
  handler: (CO) -> WorkflowAction<S, O>
): R = renderChild(workflow.asScreenWorkflow(), input, workflow::class.jvmName + key, handler)

enum class NavigationState {
  RUNNING,
  GO_FORWARD
}

private fun <I, O : Any, R, D : Any> SimpleScreenWorkflow<I, O, R, D>.asScreenWorkflow()
    : ScreenWorkflow<I, O, R, D> =
  object : StatefulWorkflow<ScreenInput<I>, NavigationState, O, ScreenRendering<R, D>>() {

    override fun initialState(
      input: ScreenInput<I>,
      snapshot: Snapshot?
    ) = snapshot?.bytes?.parse { it.readEnumByOrdinal() } ?: RUNNING

    override fun render(
      input: ScreenInput<I>,
      state: NavigationState,
      context: RenderContext<NavigationState, O>
    ): ScreenRendering<R, D> {
      val (rendering, display) = context.renderChild(this@asScreenWorkflow, input) { it }

      val goBackHandler =
        if (state == GO_FORWARD) {
          GoBackHandler(
              targetDescription = this@asScreenWorkflow::class.jvmName,
              goBack = context.onEvent { enterState(RUNNING) })
        } else null

      return ScreenRendering(rendering, display, goBackHandler)
    }

    override fun snapshotState(state: NavigationState): Snapshot = Snapshot.write {
      it.writeEnumByOrdinal(state)
    }
  }
