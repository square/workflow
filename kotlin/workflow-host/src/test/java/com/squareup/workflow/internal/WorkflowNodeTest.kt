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
package com.squareup.workflow.internal

import com.squareup.workflow.ChannelUpdate
import com.squareup.workflow.ChannelUpdate.Closed
import com.squareup.workflow.ChannelUpdate.Value
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.makeUnitSink
import com.squareup.workflow.onReceive
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.withTimeout
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class WorkflowNodeTest {

  private interface StringWorkflow : Workflow<String, String, String, String> {
    override fun snapshotState(state: String): Snapshot = fail("not expected")
    override fun restoreState(snapshot: Snapshot): String = fail("not expected")
  }

  private class InputRenderingWorkflow(
    private val onInputChanged: (String, String, String) -> String
  ) : StringWorkflow {

    override fun initialState(input: String): String {
      return "starting:$input"
    }

    override fun onInputChanged(
      old: String,
      new: String,
      state: String
    ): String = onInputChanged.invoke(old, new, state)

    override fun compose(
      input: String,
      state: String,
      context: WorkflowContext<String, String>
    ): String {
      return """
        input:$input
        state:$state
      """.trimIndent()
    }
  }

  private val context: CoroutineContext = Dispatchers.Unconfined

  @Test fun `inputs are passed to on changed`() {
    val oldAndNewInputs = mutableListOf<Pair<String, String>>()
    val workflow = InputRenderingWorkflow { old, new, state ->
      oldAndNewInputs += old to new
      state
    }
    val node = WorkflowNode(workflow.id(), workflow, "foo", null, context)

    node.compose(workflow, "foo2")

    assertEquals(listOf("foo" to "foo2"), oldAndNewInputs)
  }

  @Test fun `inputs are rendered`() {
    val workflow = InputRenderingWorkflow { old, new, _ ->
      "$old->$new"
    }
    val node = WorkflowNode(workflow.id(), workflow, "foo", null, context)

    val rendering = node.compose(workflow, "foo2")

    assertEquals(
        """
          input:foo2
          state:foo->foo2
        """.trimIndent(), rendering
    )

    val rendering2 = node.compose(workflow, "foo3")

    assertEquals(
        """
          input:foo3
          state:foo2->foo3
        """.trimIndent(), rendering2
    )
  }

  @Test fun `accepts event`() {
    lateinit var eventHandler: (String) -> Unit
    val workflow = object : StringWorkflow {
      override fun initialState(input: String): String = input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        eventHandler = context.makeSink { event -> emitOutput(event) }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    node.compose(workflow, "")
    eventHandler("event")
    val result = runBlocking {
      withTimeout(10) {
        select<String?> {
          node.tick(this) { "tick:$it" }
        }
      }
    }
    assertEquals("tick:event", result)
  }

  @Test fun `throws on subsequent events on same rendering`() {
    lateinit var eventHandler: (String) -> Unit
    val workflow = object : StringWorkflow {
      override fun initialState(input: String): String = input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        eventHandler = context.makeSink { event -> emitOutput(event) }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    node.compose(workflow, "")
    eventHandler("event")

    val e = assertFailsWith<IllegalStateException> {
      eventHandler("event2")
    }
    val expectedMessagePrefix =
      "Expected to successfully deliver event. Are you using an old rendering?\n" +
          "\tevent=event2\n" +
          "\tupdate=WorkflowAction(emitOutput(event2))@"
    assertTrue(
        e.message!!.startsWith(expectedMessagePrefix),
        "Expected\n\t${e.message}\nto start with\n\t$expectedMessagePrefix"
    )
  }

  @Test fun `subscriptions detects value`() {
    val channel = Channel<String>(capacity = 1)
    var update: ChannelUpdate<String>? = null
    val workflow = object : StringWorkflow {
      override fun initialState(input: String): String = input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        context.onReceive({ channel }) {
          check(update == null)
          update = it
          emitOutput("update:$it")
        }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    assertEquals(null, update)
    node.compose(workflow, "")
    assertEquals(null, update)

    // Shouldn't have the update yet, since we haven't sent anything.
    val output = runBlocking {
      try {
        withTimeout(1) {
          select<String?> {
            node.tick(this) { it }
          }
        }
        fail("Expected exception")
      } catch (e: TimeoutCancellationException) {
        // Expected.
      }

      channel.send("element")

      withTimeout(1) {
        select<String?> {
          node.tick(this) { it }
        }
      }
    }

    assertEquals(Value("element"), update)
    assertEquals("update:${Value("element")}", output)
  }

  @Test fun `subscriptions detects close`() {
    val channel = Channel<String>(capacity = 0)
    var update: ChannelUpdate<String>? = null
    val workflow = object : StringWorkflow {
      override fun initialState(input: String): String = input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        context.onReceive({ channel }) {
          check(update == null)
          update = it
          emitOutput("update:$it")
        }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    assertEquals(null, update)
    node.compose(workflow, "")
    assertEquals(null, update)

    channel.close()

    val output = runBlocking {
      withTimeout(1) {
        select<String?> {
          node.tick(this) { it }
        }
      }
    }

    assertEquals(Closed, update)
    assertEquals("update:$Closed", output)
  }

  @Test fun `subscriptions unsubscribes`() {
    val channel = Channel<String>(capacity = 0)
    lateinit var doClose: () -> Unit
    val workflow = object : StringWorkflow {
      override fun initialState(input: String): String = input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        when (state) {
          "listen" -> {
            context.onReceive({ channel }) {
              emitOutput("update:$it")
            }
            doClose = context.makeUnitSink {
              enterState("finished")
            }
          }
        }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "listen", null, context)

    runBlocking {
      node.compose(workflow, "listen")
      assertFalse(channel.isClosedForSend)
      doClose()

      // This tick will process the event handler, it won't close the channel yet.
      withTimeout(1) {
        select<String?> {
          node.tick(this) { it }
        }
      }

      assertFalse(channel.isClosedForSend)

      // This should close the channel.
      node.compose(workflow, "")

      assertTrue(channel.isClosedForSend)
    }
  }
}
