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

import com.squareup.workflow.legacy.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.RealWorkflowContext.Composer
import com.squareup.workflow.internal.RealWorkflowContextTest.TestComposer.Rendering
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class RealWorkflowContextTest {

  private class TestComposer : Composer<String, String> {

    data class Rendering(
      val case: WorkflowOutputCase<*, *, *, *>,
      val child: Workflow<*, *, *, *>,
      val id: WorkflowId<*, *, *, *>,
      val input: Any
    )

    @Suppress("UNCHECKED_CAST")
    override fun <IC : Any, SC : Any, OC : Any, RC : Any> compose(
      case: WorkflowOutputCase<IC, OC, String, String>,
      child: Workflow<IC, SC, OC, RC>,
      id: WorkflowId<IC, SC, OC, RC>,
      input: IC
    ): RC {
      return Rendering(case, child, id, input) as RC
    }
  }

  private class TestWorkflow : Workflow<String, String, String, Rendering> {
    override fun initialState(input: String): String = fail()

    override fun compose(
      input: String,
      state: String,
      context: WorkflowContext<String, String>
    ): Rendering {
      fail("This shouldn't actually be called.")
    }

    override fun snapshotState(state: String): Snapshot = fail()
    override fun restoreState(snapshot: Snapshot): String = fail()
  }

  private class PoisonComposer<S : Any, O : Any> : Composer<S, O> {
    override fun <IC : Any, SC : Any, OC : Any, RC : Any> compose(
      case: WorkflowOutputCase<IC, OC, S, O>,
      child: Workflow<IC, SC, OC, RC>,
      id: WorkflowId<IC, SC, OC, RC>,
      input: IC
    ): RC = fail()
  }

  @Test fun makeSink_completesUpdate() {
    val context = RealWorkflowContext<String, String>(PoisonComposer())
    val expectedUpdate = noop<String, String>()
    val handler = context.makeSink<String> { expectedUpdate }
    assertFalse(context.buildBehavior().nextActionFromEvent.isCompleted)

    handler("")

    val behavior = context.buildBehavior()
    assertTrue(behavior.nextActionFromEvent.isCompleted)
    val actualUpdate = behavior.nextActionFromEvent.getCompleted()
    assertSame(expectedUpdate, actualUpdate)
  }

  @Test fun makeSink_getsEvent() {
    val context = RealWorkflowContext<String, String>(PoisonComposer())
    val handler = context.makeSink<String> { event -> emitOutput(event) }

    handler("foo")

    val behavior = context.buildBehavior()
    val update = behavior.nextActionFromEvent.getCompleted()
    val (state, output) = update("state")
    assertEquals("state", state)
    assertEquals("foo", output)
  }

  @Test fun compose_works() {
    val context = RealWorkflowContext(TestComposer())
    val workflow = TestWorkflow()

    val (case, child, id, input) = context.compose(workflow, "input", "key") { output ->
      emitOutput("output:$output")
    }

    assertSame(workflow, child)
    assertEquals(workflow.id("key"), id)
    assertEquals("input", input)
    assertEquals<Workflow<*, *, *, *>>(workflow, case.workflow)
    assertEquals(workflow.id("key"), case.id)
    assertEquals("input", case.input)

    @Suppress("UNCHECKED_CAST")
    case as WorkflowOutputCase<String, String, String, String>
    val (state, output) = case.handler.invoke("output").invoke("state")
    assertEquals("state", state)
    assertEquals("output:output", output)

    val childCases = context.buildBehavior()
        .childCases
    assertEquals(1, childCases.size)
    assertSame(case, childCases.single())
  }
}
