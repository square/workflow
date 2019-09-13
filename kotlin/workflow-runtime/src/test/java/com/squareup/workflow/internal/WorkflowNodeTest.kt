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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.internal

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.asWorker
import com.squareup.workflow.makeEventSink
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.renderChild
import com.squareup.workflow.stateful
import com.squareup.workflow.writeUtf8WithLength
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

  private class PropsRenderingWorkflow(
    private val onPropsChanged: (String, String, String) -> String
  ) : StringWorkflow() {

    override fun initialState(
      props: String,
      snapshot: Snapshot?
    ): String {
      assertNull(snapshot)
      return "starting:$props"
    }

    override fun onPropsChanged(
      old: String,
      new: String,
      state: String
    ): String = onPropsChanged.invoke(old, new, state)

    override fun render(
      props: String,
      state: String,
      context: RenderContext<String, String>
    ): String {
      return """
        props:$props
        state:$state
      """.trimIndent()
    }
  }

  private val context: CoroutineContext = Unconfined

  @Test fun `props are passed to on changed`() {
    val oldAndNewProps = mutableListOf<Pair<String, String>>()
    val workflow = PropsRenderingWorkflow { old, new, state ->
      oldAndNewProps += old to new
      state
    }
    val node = WorkflowNode(workflow.id(), workflow, "foo", null, context)

    node.render(workflow, "foo2")

    assertEquals(listOf("foo" to "foo2"), oldAndNewProps)
  }

  @Test fun `props are rendered`() {
    val workflow = PropsRenderingWorkflow { old, new, _ ->
      "$old->$new"
    }
    val node = WorkflowNode(workflow.id(), workflow, "foo", null, context)

    val rendering = node.render(workflow, "foo2")
        .rendering

    assertEquals(
        """
          props:foo2
          state:foo->foo2
        """.trimIndent(), rendering
    )

    val rendering2 = node.render(workflow, "foo3")
        .rendering

    assertEquals(
        """
          props:foo3
          state:foo2->foo3
        """.trimIndent(), rendering2
    )
  }

  @Test fun `accepts event`() {
    lateinit var sink: Sink<String>
    val workflow = object : StringWorkflow() {
      override fun initialState(
        props: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return props
      }

      override fun render(
        props: String,
        state: String,
        context: RenderContext<String, String>
      ): String {
        sink = context.makeEventSink { it }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    node.render(workflow, "")
    sink.send("event")
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
    lateinit var sink: Sink<String>
    val workflow = object : StringWorkflow() {
      override fun initialState(
        props: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return props
      }

      override fun render(
        props: String,
        state: String,
        context: RenderContext<String, String>
      ): String {
        sink = context.makeEventSink { it }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    node.render(workflow, "")
    sink.send("event")

    val e = assertFailsWith<IllegalStateException> {
      sink.send("event2")
    }
    assertTrue(e.message!!.startsWith("Expected to successfully deliver "))
    assertTrue(e.message!!.endsWith("Are you using an old rendering?"))
  }

  @Test fun `worker gets value`() {
    val channel = Channel<String>(capacity = 1)
    var update: String? = null
    val workflow = object : StringWorkflow() {
      override fun initialState(
        props: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return props
      }

      override fun render(
        props: String,
        state: String,
        context: RenderContext<String, String>
      ): String {
        context.runningWorker(channel.asWorker()) {
          check(update == null)
          update = it
          WorkflowAction { "update:$it" }
        }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    assertEquals(null, update)
    node.render(workflow, "")
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

    assertEquals("element", update)
    assertEquals("update:element", output)
  }

  @Test fun `worker is cancelled`() {
    val channel = Channel<String>(capacity = 0)
    lateinit var doClose: () -> Unit
    val workflow = object : StringWorkflow() {
      override fun initialState(
        props: String,
        snapshot: Snapshot?
      ): String {
        assertNull(snapshot)
        return props
      }

      fun update(value: String) = WorkflowAction<String, String> {
        "update:$value"
      }

      val finish = WorkflowAction<String, String> {
        state = "finished"
        null
      }

      override fun render(
        props: String,
        state: String,
        context: RenderContext<String, String>
      ): String {
        val sink = context.makeActionSink<WorkflowAction<String, String>>()

        when (state) {
          "listen" -> {
            context.runningWorker(channel.asWorker(closeOnCancel = true)) {
              update(it)
            }
            doClose = { sink.send(finish) }
          }
        }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "listen", null, context)

    runBlocking {
      node.render(workflow, "listen")
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
      node.render(workflow, "")

      assertTrue(channel.isClosedForSend)
    }
  }

  @Test fun `snapshots non-empty without children`() {
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, snapshot ->
          snapshot?.bytes?.parse {
            it.readUtf8WithLength()
                .removePrefix("state:")
          } ?: props
        },
        render = { _, state -> state },
        snapshot = { state ->
          Snapshot.write {
            it.writeUtf8WithLength("state:$state")
          }
        }
    )
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "initial props",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial props", originalNode.render(workflow, "foo").rendering)
    val snapshot = originalNode.snapshot(workflow)
    assertNotEquals(0, snapshot.bytes.size)

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        // These props should be ignored, since snapshot is non-null.
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("initial props", restoredNode.render(workflow, "foo").rendering)
  }

  @Test fun `snapshots empty without children`() {
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, snapshot -> if (snapshot != null) "restored" else props },
        render = { _, state -> state },
        snapshot = { Snapshot.EMPTY }
    )
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "initial props",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial props", originalNode.render(workflow, "foo").rendering)
    val snapshot = originalNode.snapshot(workflow)
    assertNotEquals(0, snapshot.bytes.size)

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        // These props should be ignored, since snapshot is non-null.
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("restored", restoredNode.render(workflow, "foo").rendering)
  }

  @Test fun `snapshots non-empty with children`() {
    var restoredChildState: String? = null
    var restoredParentState: String? = null
    val childWorkflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, snapshot ->
          snapshot?.bytes?.parse {
            it.readUtf8WithLength()
                .removePrefix("child state:")
                .also { state -> restoredChildState = state }
          } ?: props
        },
        render = { _, state -> state },
        snapshot = { state ->
          Snapshot.write {
            it.writeUtf8WithLength("child state:$state")
          }
        }
    )
    val parentWorkflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, snapshot ->
          snapshot?.bytes?.parse {
            it.readUtf8WithLength()
                .removePrefix("parent state:")
                .also { state -> restoredParentState = state }
          } ?: props
        },
        render = { _, state -> "$state|" + renderChild(childWorkflow, "child props") },
        snapshot = { state ->
          Snapshot.write {
            it.writeUtf8WithLength("parent state:$state")
          }
        }
    )

    val originalNode = WorkflowNode(
        parentWorkflow.id(),
        parentWorkflow,
        initialProps = "initial props",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial props|child props", originalNode.render(parentWorkflow, "foo").rendering)
    val snapshot = originalNode.snapshot(parentWorkflow)
    assertNotEquals(0, snapshot.bytes.size)

    val restoredNode = WorkflowNode(
        parentWorkflow.id(),
        parentWorkflow,
        // These props should be ignored, since snapshot is non-null.
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("initial props|child props", restoredNode.render(parentWorkflow, "foo").rendering)
    assertEquals("child props", restoredChildState)
    assertEquals("initial props", restoredParentState)
  }

  @Test fun `snapshot counts`() {
    var snapshotCalls = 0
    var restoreCalls = 0
    // Track the number of times the snapshot is actually serialized, not snapshotState is called.
    var snapshotWrites = 0
    val workflow = Workflow.stateful<Unit, Nothing, Unit>(
        initialState = { snapshot -> if (snapshot != null) restoreCalls++ },
        render = { Unit },
        snapshot = {
          snapshotCalls++
          Snapshot.write {
            snapshotWrites++
          }
        }
    )
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

  @Test fun `restore gets props`() {
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, snapshot ->
          snapshot?.bytes?.parse {
            // Tags the restored state with the props so we can check it.
            val deserialized = it.readUtf8WithLength()
            return@parse "props:$props|state:$deserialized"
          } ?: props
        },
        render = { _, state -> state },
        snapshot = { state -> Snapshot.write { it.writeUtf8WithLength(state) } }
    )
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "initial props",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial props", originalNode.render(workflow, "foo").rendering)
    val snapshot = originalNode.snapshot(workflow)
    assertNotEquals(0, snapshot.bytes.size)

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals(
        "props:new props|state:initial props",
        restoredNode.render(workflow, "foo").rendering
    )
  }

  @Test fun `rendering generates debug snapshots`() {
    val child = PropsRenderingWorkflow { _, _, state -> state }

    class TestWorkflow : StringWorkflow() {
      override fun initialState(
        props: String,
        snapshot: Snapshot?
      ): String = "initial state"

      override fun render(
        props: String,
        state: String,
        context: RenderContext<String, String>
      ): String {
        val childRendering = context.renderChild(child, props = "child props", key = "key") {
          fail()
        }
        return "child:\n${childRendering.prependIndent()}"
      }
    }

    val workflow = TestWorkflow()
    val node = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "initial props",
        snapshot = null,
        baseContext = Unconfined
    )

    val debugSnapshot = node.render(workflow, "props")
        .debugSnapshot

    assertEquals(TestWorkflow::class.java.name, debugSnapshot.workflowType)
    assertEquals("props", debugSnapshot.props)
    assertEquals("initial state", debugSnapshot.state)
    assertEquals(
        "child:\n" +
            "    props:child props\n" +
            "    state:starting:child props",
        debugSnapshot.rendering
    )
    assertEquals(1, debugSnapshot.children.size)
    with(debugSnapshot.children.single()) {
      assertEquals("key", key)
      assertEquals(PropsRenderingWorkflow::class.java.name, snapshot.workflowType)
      assertEquals("child props", snapshot.props)
      assertEquals("starting:child props", snapshot.state)
      assertEquals(
          "props:child props\n" +
              "state:starting:child props",
          snapshot.rendering
      )
      assertTrue(snapshot.children.isEmpty())
    }
  }
}
