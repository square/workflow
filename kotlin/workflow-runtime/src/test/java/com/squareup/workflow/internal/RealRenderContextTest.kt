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
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.applyTo
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.RealRenderContext.Renderer
import com.squareup.workflow.internal.RealRenderContextTest.TestRenderer.Rendering
import com.squareup.workflow.makeEventSink
import com.squareup.workflow.renderChild
import com.squareup.workflow.runningWorker
import com.squareup.workflow.stateless
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class RealRenderContextTest {

  private class TestRenderer : Renderer<String, String> {

    data class Rendering(
      val case: WorkflowOutputCase<*, *, *, *>,
      val child: Workflow<*, *, *>,
      val id: WorkflowId<*, *, *>,
      val props: Any?
    )

    @Suppress("UNCHECKED_CAST")
    override fun <IC, OC : Any, RC> render(
      case: WorkflowOutputCase<IC, OC, String, String>,
      child: Workflow<IC, OC, RC>,
      id: WorkflowId<IC, OC, RC>,
      props: IC
    ): RC {
      return Rendering(case, child, id, props) as RC
    }
  }

  private class TestWorkflow : StatefulWorkflow<String, String, String, Rendering>() {
    override fun initialState(
      props: String,
      snapshot: Snapshot?
    ): String = fail()

    override fun render(
      props: String,
      state: String,
      context: RenderContext<String, String>
    ): Rendering {
      fail("This shouldn't actually be called.")
    }

    override fun snapshotState(state: String): Snapshot = fail()
  }

  private class PoisonRenderer<S, O : Any> : Renderer<S, O> {
    override fun <IC, OC : Any, RC> render(
      case: WorkflowOutputCase<IC, OC, S, O>,
      child: Workflow<IC, OC, RC>,
      id: WorkflowId<IC, OC, RC>,
      props: IC
    ): RC = fail()
  }

  @Test fun `make sink completes update`() {
    val context = RealRenderContext<String, String>(PoisonRenderer())
    val expectedUpdate = noAction<String, String>()
    val handler = context.onEvent<String> { expectedUpdate }
    val behavior = context.buildBehavior()
    assertFalse(behavior.nextActionFromEvent.isCompleted)

    handler("")

    assertTrue(behavior.nextActionFromEvent.isCompleted)
    val actualUpdate = behavior.nextActionFromEvent.getCompleted()
    assertSame(expectedUpdate, actualUpdate)
  }

  @Test fun `make sink gets event`() {
    val context = RealRenderContext<String, String>(PoisonRenderer())
    val sink: Sink<String> = context.makeEventSink { it }
    sink.send("foo")

    val behavior = context.buildBehavior()
    val update = behavior.nextActionFromEvent.getCompleted()
    val (state, output) = update.applyTo("state")
    assertEquals("state", state)
    assertEquals("foo", output)
  }

  @Test fun `make sink works with OutputT of Nothing`() {
    val context = RealRenderContext<String, Nothing>(PoisonRenderer())
    val sink: Sink<String> = context.makeEventSink { null }
    sink.send("foo")

    val behavior = context.buildBehavior()
    val update = behavior.nextActionFromEvent.getCompleted()
    val (state, output) = update.applyTo("state")
    assertEquals("state", state)
    assertNull(output)
  }

  @Test fun `renderChild works`() {
    val context = RealRenderContext(TestRenderer())
    val workflow = TestWorkflow()

    val (case, child, id, props) = context.renderChild(workflow, "props", "key") { output ->
      WorkflowAction { "output:$output" }
    }

    assertSame(workflow, child)
    assertEquals(workflow.id("key"), id)
    assertEquals("props", props)
    assertEquals<Workflow<*, *, *>>(workflow, case.workflow)
    assertEquals(workflow.id("key"), case.id)
    assertEquals("props", case.props)

    @Suppress("UNCHECKED_CAST")
    case as WorkflowOutputCase<String, String, String, String>
    val (state, output) = case.handler.invoke("output").applyTo("state")
    assertEquals("state", state)
    assertEquals("output:output", output)

    val childCases = context.buildBehavior()
        .childCases
    assertEquals(1, childCases.size)
    assertSame(case, childCases.single())
  }

  @Test fun `all methods throw after buildBehavior`() {
    val context = RealRenderContext(TestRenderer())
    context.buildBehavior()

    assertFailsWith<IllegalStateException> { context.onEvent<Unit> { fail() } }
    val child = Workflow.stateless<Unit, Nothing, Unit> { fail() }
    assertFailsWith<IllegalStateException> { context.renderChild(child) }
    val worker = Worker.from { Unit }
    assertFailsWith<IllegalStateException> { context.runningWorker(worker) { fail() } }
    assertFailsWith<IllegalStateException> { context.buildBehavior() }
  }
}
