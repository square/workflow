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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow

import com.squareup.workflow.testing.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LifecycleWorkerTest {

  @Test fun `onStart called immediately`() {
    var onStartCalled = false
    val worker = object : LifecycleWorker() {
      override fun onStarted() {
        onStartCalled = true
      }
    }

    assertFalse(onStartCalled)
    runBlocking {
      val job = worker.run()
          .launchIn(CoroutineScope(Unconfined))
      assertTrue(onStartCalled)

      // Don't hang the runBlocking block forever.
      job.cancel()
    }
  }

  @Test fun `onCancelled called on cancel`() {
    var onCancelledCalled = false
    val worker = object : LifecycleWorker() {
      override fun onStopped() {
        onCancelledCalled = true
      }
    }

    worker.test {
      assertFalse(onCancelledCalled)
      cancelWorker()

      assertTrue(onCancelledCalled)
    }
  }
}
