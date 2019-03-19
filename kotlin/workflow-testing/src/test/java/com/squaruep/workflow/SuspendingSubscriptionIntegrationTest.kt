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
package com.squaruep.workflow

import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.onSuspending
import com.squareup.workflow.testing.testFromStart
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.fail

class SuspendingSubscriptionIntegrationTest {

  private class ExpectedException : RuntimeException()

  @Test fun `handles value`() {
    val deferred = CompletableDeferred<String>()
    val workflow = StatelessWorkflow<String, Unit> { context ->
      context.onSuspending({ deferred.await() }, Unit) {
        emitOutput("output:$it")
      }
    }

    workflow.testFromStart { host ->
      assertFalse(host.hasOutput)

      deferred.complete("value")

      host.withNextOutput {
        assertEquals("output:value", it)
      }

      assertFalse(host.hasOutput)
    }
  }

  @Test fun `handles error`() {
    val workflow = StatelessWorkflow<String, Unit> { context ->
      context.onSuspending({ throw ExpectedException() }, Unit) {
        fail("Shouldn't get here.")
      }
    }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart { host ->
        assertFalse(host.hasOutput)

        host.awaitNextOutput()
      }
    }
  }
}
