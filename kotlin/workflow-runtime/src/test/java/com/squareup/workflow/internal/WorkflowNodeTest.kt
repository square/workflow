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

package com.squareup.workflow.internal

import com.squareup.workflow.EventHandler
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.compose
import com.squareup.workflow.invoke
import com.squareup.workflow.onReceive
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.stateless
import com.squareup.workflow.util.ChannelUpdate
import com.squareup.workflow.util.ChannelUpdate.Closed
import com.squareup.workflow.util.ChannelUpdate.Value
import com.squareup.workflow.writeUtf8WithLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class WorkflowNodeTest {

  private abstract class StringWorkflow : StatefulWorkflow<String, String, String, String>() {
    override fun snapshotState(state: String): Snapshot = fail("not expected")
  }

  private class InputRenderingWorkflow(
    private val onInputChanged: (String, String, String) -> String
  ) : StringWorkflow() {

    override fun initialState(
      input: String,
      snapshot: Snapshot?
    ): String {
      assertNull(snapshot)
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
    val workflow = object : StringWorkflow() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return input
      }

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        eventHandler = context.onEvent { event -> emitOutput(event) }
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
    val workflow = object : StringWorkflow() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return input
      }

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, String>
      ): String {
        eventHandler = context.onEvent { event -> emitOutput(event) }
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
    val workflow = object : StringWorkflow() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return input
      }

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
    val workflow = object : StringWorkflow() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return input
      }

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
    lateinit var doClose: EventHandler<Unit>
    val workflow = object : StringWorkflow() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return input
      }

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
            doClose = context.onEvent {
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

  @Test fun `snapshots non-empty without children`() {
    val workflow = object : StatefulWorkflow<String, String, Nothing, String>() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String = snapshot?.bytes?.parse {
        it.readUtf8WithLength()
            .removePrefix("state:")
      } ?: input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, Nothing>
      ): String = state

      override fun snapshotState(state: String): Snapshot = Snapshot.write {
        it.writeUtf8WithLength("state:$state")
      }
    }
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialInput = "initial input",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial input", originalNode.compose(workflow, "foo"))
    val snapshot = originalNode.snapshot(workflow)
    assertNotEquals(0, snapshot.bytes.size())

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        // This input should be ignored, since snapshot is non-null.
        initialInput = "new input",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("initial input", restoredNode.compose(workflow, "foo"))
  }

  @Test fun `snapshots empty without children`() {
    val workflow = object : StatefulWorkflow<String, String, Nothing, String>() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String = if (snapshot != null) "restored" else input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, Nothing>
      ): String {
        return state
      }

      override fun snapshotState(state: String): Snapshot = Snapshot.EMPTY
    }
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialInput = "initial input",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial input", originalNode.compose(workflow, "foo"))
    val snapshot = originalNode.snapshot(workflow)
    assertNotEquals(0, snapshot.bytes.size())

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        // This input should be ignored, since snapshot is non-null.
        initialInput = "new input",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("restored", restoredNode.compose(workflow, "foo"))
  }

  @Test fun `snapshots non-empty with children`() {
    var restoredChildState: String? = null
    var restoredParentState: String? = null
    val childWorkflow = object : StatefulWorkflow<String, String, Nothing, String>() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String = snapshot?.bytes?.parse {
        it.readUtf8WithLength()
            .removePrefix("child state:")
            .also { state -> restoredChildState = state }
      } ?: input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, Nothing>
      ): String {
        return state
      }

      override fun snapshotState(state: String): Snapshot = Snapshot.write {
        it.writeUtf8WithLength("child state:$state")
      }
    }
    val parentWorkflow = object : StatefulWorkflow<String, String, Nothing, String>() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String = snapshot?.bytes?.parse {
        it.readUtf8WithLength()
            .removePrefix("parent state:")
            .also { state -> restoredParentState = state }
      } ?: input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, Nothing>
      ): String {
        val childRendering = context.compose(childWorkflow, "child input")
        return "$state|$childRendering"
      }

      override fun snapshotState(state: String): Snapshot = Snapshot.write {
        it.writeUtf8WithLength("parent state:$state")
      }
    }

    val originalNode = WorkflowNode(
        parentWorkflow.id(),
        parentWorkflow,
        initialInput = "initial input",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial input|child input", originalNode.compose(parentWorkflow, "foo"))
    val snapshot = originalNode.snapshot(parentWorkflow)
    assertNotEquals(0, snapshot.bytes.size())

    val restoredNode = WorkflowNode(
        parentWorkflow.id(),
        parentWorkflow,
        // This input should be ignored, since snapshot is non-null.
        initialInput = "new input",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("initial input|child input", restoredNode.compose(parentWorkflow, "foo"))
    assertEquals("child input", restoredChildState)
    assertEquals("initial input", restoredParentState)
  }

  @Test fun `snapshot counts`() {
    var snapshotCalls = 0
    var restoreCalls = 0
    // Track the number of times the snapshot is actually serialized, not snapshotState is called.
    var snapshotWrites = 0
    val workflow = object : StatefulWorkflow<Unit, Unit, Nothing, Unit>() {
      override fun initialState(
        input: Unit,
        snapshot: Snapshot?
      ) {
        if (snapshot != null) {
          restoreCalls++
        }
      }

      override fun compose(
        input: Unit,
        state: Unit,
        context: WorkflowContext<Unit, Nothing>
      ) = Unit

      override fun snapshotState(state: Unit): Snapshot {
        snapshotCalls++
        return Snapshot.write {
          snapshotWrites++
        }
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, Unit, null, Unconfined)

    assertEquals(0, snapshotCalls)
    assertEquals(0, snapshotWrites)
    assertEquals(0, restoreCalls)

    val snapshot = node.snapshot(workflow)

    assertEquals(1, snapshotCalls)
    assertEquals(0, snapshotWrites)
    assertEquals(0, restoreCalls)

    snapshot.bytes

    assertEquals(1, snapshotCalls)
    assertEquals(1, snapshotWrites)
    assertEquals(0, restoreCalls)

    WorkflowNode(workflow.id(), workflow, Unit, snapshot, Unconfined)

    assertEquals(1, snapshotCalls)
    assertEquals(1, snapshotWrites)
    assertEquals(1, restoreCalls)
  }

  @Test fun `restore gets input`() {
    val workflow = object : StatefulWorkflow<String, String, Nothing, String>() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String = snapshot?.bytes?.parse {
        // Tags the restored state with the input so we can check it.
        val deserialized = it.readUtf8WithLength()
        return@parse "input:$input|state:$deserialized"
      } ?: input

      override fun compose(
        input: String,
        state: String,
        context: WorkflowContext<String, Nothing>
      ): String {
        return state
      }

      override fun snapshotState(state: String): Snapshot = Snapshot.write {
        it.writeUtf8WithLength(state)
      }
    }
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialInput = "initial input",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial input", originalNode.compose(workflow, "foo"))
    val snapshot = originalNode.snapshot(workflow)
    assertNotEquals(0, snapshot.bytes.size())

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialInput = "new input",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("input:new input|state:initial input", restoredNode.compose(workflow, "foo"))
  }

  @Test fun `invokes teardown hooks in order on cancel`() {
    val teardowns = mutableListOf<Int>()
    val workflow = Workflow.stateless<Nothing, Unit> { context ->
      context.onTeardown { teardowns += 1 }
      context.onTeardown { teardowns += 2 }
    }
    val node = WorkflowNode(workflow.id(), workflow.asStatefulWorkflow(), Unit, null, Unconfined)
    node.compose(workflow.asStatefulWorkflow(), Unit)

    assertTrue(teardowns.isEmpty())

    node.cancel()

    assertEquals(listOf(1, 2), teardowns)
  }
}
