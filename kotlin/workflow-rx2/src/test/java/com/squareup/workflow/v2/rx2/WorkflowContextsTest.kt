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
package com.squareup.workflow.v2.rx2

import com.squareup.workflow.legacy.Snapshot
import com.squareup.workflow.v2.StatelessWorkflow
import com.squareup.workflow.v2.Workflow
import com.squareup.workflow.v2.WorkflowContext
import com.squareup.workflow.v2.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.v2.WorkflowAction.Companion.enterState
import com.squareup.workflow.v2.WorkflowAction.Companion.noop
import com.squareup.workflow.v2.makeUnitSink
import com.squareup.workflow.v2.testing.testFromStart
import io.reactivex.subjects.SingleSubject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class WorkflowContextsTest {

  @Test fun onSuccess_handlesValue_whenEmittedDuringListen() {
    var subscriptions = 0
    var disposals = 0
    val singleSubject = SingleSubject.create<String>()
    val single = singleSubject
        .doOnSubscribe { subscriptions++ }
        .doOnDispose { disposals++ }
    val workflow = StatelessWorkflow<String, Unit> { context ->
      context.onSuccess(single) { emitOutput(it) }
    }

    assertEquals(0, subscriptions)
    assertEquals(0, disposals)

    workflow.testFromStart { host ->
      assertEquals(1, subscriptions)
      assertEquals(0, disposals)
      assertFalse(host.hasOutput)

      singleSubject.onSuccess("done!")

      assertTrue(host.hasOutput)
      assertEquals("done!", host.awaitNextOutput())
      assertEquals(1, subscriptions)
      assertEquals(0, disposals)
    }
  }

  @Test fun onSuccess_unsubscribes() {
    var subscriptions = 0
    var disposals = 0
    lateinit var doClose: () -> Unit
    val singleSubject = SingleSubject.create<Unit>()
    val single = singleSubject
        .doOnSubscribe { subscriptions++ }
        .doOnDispose { disposals++ }
    val workflow = object : Workflow<Unit, Boolean, Nothing, Unit> {
      override fun initialState(input: Unit): Boolean = true

      override fun compose(
        input: Unit,
        state: Boolean,
        context: WorkflowContext<Boolean, Nothing>
      ) {
        if (state) {
          context.onSuccess(single) { noop() }
          doClose = context.makeUnitSink { enterState(false) }
        }
      }

      override fun snapshotState(state: Boolean): Snapshot = Snapshot.EMPTY
      override fun restoreState(snapshot: Snapshot): Boolean = fail("not expected")
    }

    assertEquals(0, subscriptions)
    assertEquals(0, disposals)

    workflow.testFromStart { host ->
      assertEquals(1, subscriptions)
      assertEquals(0, disposals)
      assertFalse(host.hasOutput)

      doClose()

      assertEquals(1, subscriptions)
      assertEquals(1, disposals)
    }
  }
}
