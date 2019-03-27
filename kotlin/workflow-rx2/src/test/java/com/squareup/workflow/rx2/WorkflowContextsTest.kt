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
package com.squareup.workflow.rx2

import com.squareup.workflow.EventHandler
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.invoke
import com.squareup.workflow.testing.testFromStart
import io.reactivex.subjects.SingleSubject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkflowContextsTest {

  @Test fun `onSuccess handles value when emitted during listen`() {
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

  @Test fun `onSuccess unsubscribes`() {
    var subscriptions = 0
    var disposals = 0
    lateinit var doClose: EventHandler<Unit>
    val singleSubject = SingleSubject.create<Unit>()
    val single = singleSubject
        .doOnSubscribe { subscriptions++ }
        .doOnDispose { disposals++ }
    val workflow = object : StatefulWorkflow<Unit, Boolean, Nothing, Unit>() {
      override fun initialState(
        input: Unit,
        snapshot: Snapshot?
      ): Boolean = true

      override fun compose(
        input: Unit,
        state: Boolean,
        context: WorkflowContext<Boolean, Nothing>
      ) {
        if (state) {
          context.onSuccess(single) { noop() }
          doClose = context.onEvent { enterState(false) }
        }
      }

      override fun snapshotState(state: Boolean): Snapshot = Snapshot.EMPTY
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
