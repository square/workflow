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
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.action
import com.squareup.workflow.asWorker
import com.squareup.workflow.contraMap
import com.squareup.workflow.makeEventSink
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.renderChild
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import com.squareup.workflow.writeByteStringWithLength
import com.squareup.workflow.writeUtf8WithLength
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
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

  @Test fun `onPropsChanged is called when props change`() {
    val oldAndNewProps = mutableListOf<Pair<String, String>>()
    val workflow = PropsRenderingWorkflow { old, new, state ->
      oldAndNewProps += old to new
      return@PropsRenderingWorkflow state
    }
    val node = WorkflowNode(workflow.id(), workflow, "old", null, context)

    node.render(workflow, "new")

    assertEquals(listOf("old" to "new"), oldAndNewProps)
  }

  @Test fun `onPropsChanged is not called when props are equal`() {
    val oldAndNewProps = mutableListOf<Pair<String, String>>()
    val workflow = PropsRenderingWorkflow { old, new, state ->
      oldAndNewProps += old to new
      return@PropsRenderingWorkflow state
    }
    val node = WorkflowNode(workflow.id(), workflow, "old", null, context)

    node.render(workflow, "old")

    assertTrue(oldAndNewProps.isEmpty())
  }

  @Test fun `props are rendered`() {
    val workflow = PropsRenderingWorkflow { old, new, _ ->
      "$old->$new"
    }
    val node = WorkflowNode(workflow.id(), workflow, "foo", null, context)

    val rendering = node.render(workflow, "foo2")

    assertEquals(
        """
          props:foo2
          state:foo->foo2
        """.trimIndent(), rendering
    )

    val rendering2 = node.render(workflow, "foo3")

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
        sink = context.makeEventSink { setOutput(it) }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context, { "tick:$it" })
    node.render(workflow, "")

    sink.send("event")

    val result = runBlocking {
      withTimeout(10) {
        select<String?> {
          node.tick(this)
        }
      }
    }
    assertEquals("tick:event", result)
  }

  @Test fun `accepts events sent to stale renderings`() {
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
        sink = context.makeEventSink { setOutput(it) }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context, { "tick:$it" })
    node.render(workflow, "")

    sink.send("event")
    sink.send("event2")

    val result = runBlocking {
      withTimeout(10) {
        List(2) {
          select<String?> {
            node.tick(this)
          }
        }
      }
    }
    assertEquals(listOf("tick:event", "tick:event2"), result)
  }

  @Test fun `send allows subsequent events on same rendering`() {
    lateinit var sink: Sink<WorkflowAction<String, String>>
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
        sink = context.actionSink
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    node.render(workflow, "")
    sink.send(emitOutput("event"))

    // Should not throw.
    sink.send(emitOutput("event2"))
  }

  @Test fun `onEvent allows subsequent events on same rendering`() {
    lateinit var sink: (WorkflowAction<String, String>) -> Unit
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
        sink = context.onEvent { it }
        return ""
      }
    }
    val node = WorkflowNode(workflow.id(), workflow, "", null, context)

    node.render(workflow, "")
    sink(emitOutput("event"))

    // Should not throw.
    sink(emitOutput("event2"))
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
          action { setOutput("update:$it") }
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
            node.tick(this)
          }
        }
        fail("Expected exception")
      } catch (e: TimeoutCancellationException) {
        // Expected.
      }

      channel.send("element")

      withTimeout(1) {
        select<String?> {
          node.tick(this)
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

      fun update(value: String) = action<String, String> {
        setOutput("update:$value")
      }

      val finish = action<String, String> {
        nextState = "finished"
      }

      override fun render(
        props: String,
        state: String,
        context: RenderContext<String, String>
      ): String {
        when (state) {
          "listen" -> {
            context.runningWorker(channel.asWorker(closeOnCancel = true)) {
              update(it)
            }
            doClose = { context.actionSink.send(finish) }
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
          node.tick(this)
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

    assertEquals("initial props", originalNode.render(workflow, "foo"))
    val snapshot = originalNode.snapshot(workflow)
        .bytes
    assertNotEquals(0, snapshot.size)

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        // These props should be ignored, since snapshot is non-null.
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("initial props", restoredNode.render(workflow, "foo"))
  }

  @Test fun `snapshots empty without children`() {
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, snapshot -> snapshot?.bytes?.utf8() ?: props },
        render = { _, state -> state },
        snapshot = { Snapshot.of("restored") }
    )
    val originalNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "initial props",
        snapshot = null,
        baseContext = Unconfined
    )

    assertEquals("initial props", originalNode.render(workflow, "foo"))
    val snapshot = originalNode.snapshot(workflow)
        .bytes
    assertNotEquals(0, snapshot.size)

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        // These props should be ignored, since snapshot is non-null.
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("restored", restoredNode.render(workflow, "foo"))
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

    assertEquals("initial props|child props", originalNode.render(parentWorkflow, "foo"))
    val snapshot = originalNode.snapshot(parentWorkflow)
        .bytes
    assertNotEquals(0, snapshot.size)

    val restoredNode = WorkflowNode(
        parentWorkflow.id(),
        parentWorkflow,
        // These props should be ignored, since snapshot is non-null.
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("initial props|child props", restoredNode.render(parentWorkflow, "foo"))
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
            // Snapshot will be discarded on restoration if it's empty, so we need to write
            // something here so we actually get a non-null snapshot in restore.
            it.writeUtf8("dummy value")
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

    WorkflowNode(workflow.id(), workflow, Unit, snapshot.bytes, Unconfined)

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

    assertEquals("initial props", originalNode.render(workflow, "foo"))
    val snapshot = originalNode.snapshot(workflow)
        .bytes
    assertNotEquals(0, snapshot.size)

    val restoredNode = WorkflowNode(
        workflow.id(),
        workflow,
        initialProps = "new props",
        snapshot = snapshot,
        baseContext = Unconfined
    )
    assertEquals("props:new props|state:initial props", restoredNode.render(workflow, "foo"))
  }

  @Test fun `emits started diagnostic event not restored from snapshot`() {
    val listener = RecordingDiagnosticListener()
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, _ -> "($props:)" },
        render = { state, props -> "($props:$state)" },
        onPropsChanged = { old, new, state -> "($old:$new:$state)" },
        snapshot = { Snapshot.EMPTY }
    )

    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = "props",
        snapshot = null,
        baseContext = Unconfined,
        parentDiagnosticId = 42,
        diagnosticListener = listener
    )

    assertEquals(
        listOf(
            "onWorkflowStarted(${node.diagnosticId}, 42, " +
                "${workflow.id().typeDebugString}," +
                " , props, (props:), false)"
        ),
        listener.consumeEvents()
    )
  }

  @Test fun `emits started diagnostic event restored from snapshot`() {
    val listener = RecordingDiagnosticListener()
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, _ -> "($props:)" },
        render = { state, props -> "($props:$state)" },
        onPropsChanged = { old, new, state -> "($old:$new:$state)" },
        snapshot = { Snapshot.EMPTY }
    )
    // TODO use a valid snapshot
    val snapshot = Buffer()
        .apply {
          writeByteStringWithLength("state".encodeUtf8())
          // No children snapshots.
          writeInt(0)
        }
        .readByteString()

    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = "props",
        snapshot = snapshot,
        baseContext = Unconfined,
        parentDiagnosticId = 42,
        diagnosticListener = listener
    )

    assertEquals(
        listOf(
            "onWorkflowStarted(${node.diagnosticId}, 42," +
                " ${workflow.id().typeDebugString}," +
                " , props, (props:), true)"
        ),
        listener.consumeEvents()
    )
  }

  @Test fun `emits stopped diagnostic event`() {
    val listener = RecordingDiagnosticListener()
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { }
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined,
        parentDiagnosticId = 42,
        diagnosticListener = listener
    )
    listener.consumeEvents()

    node.cancel()

    assertEquals(listOf("onWorkflowStopped(${node.diagnosticId})"), listener.consumeEvents())
  }

  @Test fun `render emits diagnostic events`() {
    val listener = RecordingDiagnosticListener()
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { props, _ -> "($props:)" },
        render = { state, props -> "($props:$state)" },
        onPropsChanged = { old, new, state -> "($old:$new:$state)" },
        snapshot = { Snapshot.EMPTY }
    )
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = "props",
        snapshot = null,
        baseContext = Unconfined,
        parentDiagnosticId = 42,
        diagnosticListener = listener
    )
    listener.consumeEvents()

    node.render(workflow.asStatefulWorkflow(), "new props")

    assertEquals(
        listOf(
            "onPropsChanged(${node.diagnosticId}, props, new props, (props:), (props:new props:(props:)))",
            "onBeforeWorkflowRendered(${node.diagnosticId}, new props, (props:new props:(props:)))",
            "onAfterWorkflowRendered(${node.diagnosticId}, ((props:new props:(props:)):new props))"
        ), listener.consumeEvents()
    )
  }

  @Test fun `tick emits diagnostic events for worker`() {
    val listener = RecordingDiagnosticListener()
    val channel = Channel<String>()
    val action = object : WorkflowAction<String, String> {
      override fun toString(): String = "TestAction"
      override fun Updater<String, String>.apply() {
        setOutput("action output")
      }
    }
    val workflow = Workflow.stateful<String, String, String, String>(
        initialState = { props, _ -> "($props:)" },
        render = { state, props ->
          runningWorker(channel.asWorker()) { action }
          "($props:$state)"
        },
        onPropsChanged = { old, new, state -> "($old:$new:$state)" },
        snapshot = { Snapshot.EMPTY }
    )
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = "props",
        snapshot = null,
        baseContext = Unconfined,
        parentDiagnosticId = 42,
        diagnosticListener = listener
    )
    node.render(workflow.asStatefulWorkflow(), "new props")
    listener.consumeEvents()

    runBlocking {
      // Do the select in a separate coroutine so we can check the events before and after the
      // update.
      launch(start = UNDISPATCHED) {
        select<String?> {
          node.tick(this)
        }
      }
      yield()
      assertEquals(emptyList(), listener.consumeEvents())

      channel.send("foo")
    }

    assertTrue(
        """onWorkerOutput\(\d+, ${node.diagnosticId}, foo\)""".toRegex()
            .matches(listener.consumeNextEvent())
    )
    assertEquals(
        listOf(
            "onWorkflowAction(${node.diagnosticId}, TestAction, (props:new props:(props:))," +
                " (props:new props:(props:)), action output)"
        ), listener.consumeEvents()
    )
  }

  @Test fun `tick emits diagnostic events for sink`() {
    val listener = RecordingDiagnosticListener()
    fun action(value: String) = object : WorkflowAction<String, String> {
      override fun toString(): String = "TestAction"
      override fun Updater<String, String>.apply() {
        nextState = "state: $value"
        setOutput("output: $value")
      }
    }

    val workflow = Workflow.stateful<String, String, String, (String) -> Unit>(
        initialState = { props, _ -> "($props:)" },
        render = { _, _ ->
          return@stateful { actionSink.send(action(it)) }
        },
        onPropsChanged = { old, new, state -> "($old:$new:$state)" },
        snapshot = { Snapshot.EMPTY }
    )
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = "props",
        snapshot = null,
        baseContext = Unconfined,
        parentDiagnosticId = 42,
        diagnosticListener = listener
    )
    val rendering = node.render(workflow.asStatefulWorkflow(), "new props")
    listener.consumeEvents()

    runBlocking {
      // Do the select in a separate coroutine so we can check the events before and after the
      // update.
      launch(start = UNDISPATCHED) {
        select<String?> {
          node.tick(this)
        }
      }
      yield()
      assertEquals(emptyList(), listener.consumeEvents())

      rendering.invoke("foo")
    }

    assertEquals(
        listOf(
            "onSinkReceived(${node.diagnosticId}, TestAction)",
            "onWorkflowAction(0, TestAction, (props:new props:(props:))," +
                " state: foo, output: foo)"
        ), listener.consumeEvents()
    )
  }

  @Test fun `eventSink send fails before render pass completed`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      val sink: Sink<String> = makeEventSink { fail("Expected sink send to fail.") }
      sink.send("Foo")
    }
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined
    )

    val error = assertFailsWith<UnsupportedOperationException> {
      node.render(workflow.asStatefulWorkflow(), Unit)
    }
    assertTrue(
        error.message!!.startsWith(
            "Expected sink to not be sent to until after the render pass. " +
                "Received action: WorkflowAction(eventSink(Foo))@"
        )
    )
  }

  @Test fun `send fails before render pass completed`() {
    class TestAction : WorkflowAction<Nothing, Nothing> {
      override fun Updater<Nothing, Nothing>.apply() = fail("Expected sink send to fail.")
      override fun toString(): String = "TestAction()"
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      actionSink.send(TestAction())
    }
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined
    )

    val error = assertFailsWith<UnsupportedOperationException> {
      node.render(workflow.asStatefulWorkflow(), Unit)
    }
    assertEquals(
        "Expected sink to not be sent to until after the render pass. " +
            "Received action: TestAction()",
        error.message
    )
  }

  @Test fun `actionSink action changes state`() {
    val workflow = Workflow.stateful<Unit, String, Nothing, Pair<String, Sink<String>>>(
        initialState = { "initial" },
        render = { _, state ->
          state to actionSink.contraMap {
            action { nextState = "$nextState->$it" }
          }
        }
    )
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined
    )
    val (_, sink) = node.render(workflow.asStatefulWorkflow(), Unit)

    sink.send("hello")

    runBlocking {
      select<String?> {
        node.tick(this)
      }
    }

    val (state, _) = node.render(workflow.asStatefulWorkflow(), Unit)
    assertEquals("initial->hello", state)
  }

  @Test fun `actionSink action emits output`() {
    val workflow = Workflow.stateless<Unit, String, Sink<String>> {
      actionSink.contraMap { action { setOutput(it) } }
    }
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined,
        emitOutputToParent = { "output:$it" }
    )
    val rendering = node.render(workflow.asStatefulWorkflow(), Unit)

    rendering.send("hello")

    val output = runBlocking {
      select<String?> {
        node.tick(this)
      }
    }

    assertEquals("output:hello", output)
  }

  @Test fun `worker action changes state`() {
    val workflow = Workflow.stateful<Unit, String, Nothing, String>(
        initialState = { "initial" },
        render = { _, state ->
          runningWorker(Worker.from { "hello" }) { action { nextState = "$nextState->$it" } }
          return@stateful state
        }
    )
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined
    )
    node.render(workflow.asStatefulWorkflow(), Unit)

    runBlocking {
      select<String?> {
        node.tick(this)
      }
    }

    val state = node.render(workflow.asStatefulWorkflow(), Unit)
    assertEquals("initial->hello", state)
  }

  @Test fun `worker action emits output`() {
    val workflow = Worker.from { "hello" }
        .asWorkflow()
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined,
        emitOutputToParent = { "output:$it" }
    )
    node.render(workflow.asStatefulWorkflow(), Unit)

    val output = runBlocking {
      select<String?> {
        node.tick(this)
      }
    }

    assertEquals("output:hello", output)
  }

  @Test fun `child action changes state`() {
    val child = Worker.from { "hello" }
        .asWorkflow()
    val workflow = Workflow.stateful<Unit, String, Nothing, String>(
        initialState = { "initial" },
        render = { _, state ->
          renderChild(child) { action { nextState = "$nextState->$it" } }
          return@stateful state
        }
    )
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined
    )
    node.render(workflow.asStatefulWorkflow(), Unit)

    runBlocking {
      select<String?> {
        node.tick(this)
      }
    }

    val state = node.render(workflow.asStatefulWorkflow(), Unit)
    assertEquals("initial->hello", state)
  }

  @Test fun `child action emits output`() {
    val child = Worker.from { "hello" }
        .asWorkflow()
    val workflow = Workflow.stateless<Unit, String, Unit> {
      renderChild(child) { action { setOutput("child:$it") } }
    }
    val node = WorkflowNode(
        workflow.id(),
        workflow.asStatefulWorkflow(),
        initialProps = Unit,
        snapshot = null,
        baseContext = Unconfined,
        emitOutputToParent = { "output:$it" }
    )
    node.render(workflow.asStatefulWorkflow(), Unit)

    val output = runBlocking {
      select<String?> {
        node.tick(this)
      }
    }

    assertEquals("output:child:hello", output)
  }

  private fun <T : Any> Worker<T>.asWorkflow() = Workflow.stateless<Unit, T, Unit> {
    runningWorker(this@asWorkflow) { action { setOutput(it) } }
  }
}
