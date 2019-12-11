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
package com.squareup.sample.helloterminal

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.action
import kotlinx.coroutines.delay

/**
 * A simple workflow that renders either a cursor character or an empty string, changing after every
 * [delayMs] milliseconds.
 */
class BlinkingCursorWorkflow(
  cursor: Char,
  private val delayMs: Long
) : StatefulWorkflow<Unit, Boolean, Nothing, String>() {

  private val cursorString = cursor.toString()

  private val intervalWorker = Worker.create {
    var on = true
    while (true) {
      emit(on)
      delay(delayMs)
      on = !on
    }
  }

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): Boolean = true

  override fun render(
    props: Unit,
    state: Boolean,
    context: RenderContext<Boolean, Nothing>
  ): String {
    context.runningWorker(intervalWorker) { setCursorShowing(it) }
    return if (state) cursorString else ""
  }

  override fun snapshotState(state: Boolean): Snapshot = Snapshot.EMPTY

  private fun setCursorShowing(showing: Boolean) = action {
    nextState = showing
  }
}
