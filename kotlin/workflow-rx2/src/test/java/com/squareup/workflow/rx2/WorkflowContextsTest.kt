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
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.invoke
import com.squareup.workflow.stateless
import com.squareup.workflow.testing.testFromStart
import io.reactivex.subjects.SingleSubject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkflowContextsTest {

  @Test fun `onSuccess handles value when emitted during listen`() {
    val subscribed = CompletableDeferred<Unit>()
    val disposed = CompletableDeferred<Unit>()
    val singleSubject = SingleSubject.create<String>()
    val single = singleSubject
        .doOnSubscribe { subscribed.complete(Unit) }
        .doOnDispose { disposed.complete(Unit) }
    val workflow = Workflow.stateless<String, Unit> { context ->
      context.onSuccess(single) { emitOutput(it) }
    }

    assertFalse(subscribed.isCompleted)
    assertFalse(disposed.isCompleted)

    workflow.testFromStart { host ->
      host.awaitNextRendering()
      runBlocking { subscribed.await() }
      assertFalse(disposed.isCompleted)
      assertFalse(host.hasOutput)

      singleSubject.onSuccess("done!")

      host.awaitNextRendering()
      assertTrue(host.hasOutput)
      assertEquals("done!", host.awaitNextOutput())
      assertFalse(disposed.isCompleted)
    }
  }

  @Test fun `onSuccess unsubscribes`() {
    val subscribed = CompletableDeferred<Unit>()
    val disposed = CompletableDeferred<Unit>()
    lateinit var doClose: EventHandler<Unit>
    val singleSubject = SingleSubject.create<Unit>()
    val single = singleSubject
        .doOnSubscribe { subscribed.complete(Unit) }
        .doOnDispose { disposed.complete(Unit) }
    val workflow = object : StatefulWorkflow<Unit, Boolean, Nothing, Unit>() {
      override fun initialState(
        input: Unit,
        snapshot: Snapshot?,
        scope: CoroutineScope
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

    assertFalse(subscribed.isCompleted)
    assertFalse(disposed.isCompleted)

    workflow.testFromStart { host ->
      host.awaitNextRendering()
      runBlocking { subscribed.await() }
      assertFalse(disposed.isCompleted)
      assertFalse(host.hasOutput)

      doClose()

      host.awaitNextRendering()
      runBlocking { disposed.await() }
    }
  }
}
