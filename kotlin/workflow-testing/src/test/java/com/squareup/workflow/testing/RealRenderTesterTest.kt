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
package com.squareup.workflow.testing

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.WorkflowAction.Updater
import com.squareup.workflow.contraMap
import com.squareup.workflow.renderChild
import com.squareup.workflow.runningWorker
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RealRenderTesterTest {

  private interface OutputWhateverChild : Workflow<Unit, Unit, Unit>
  private interface OutputNothingChild : Workflow<Unit, Nothing, Unit>

  @Test fun `expectWorkflow with output throws when already expecting workflow output`() {
    // Don't need an implementation, the test should fail before even calling render.
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(
            OutputWhateverChild::class, rendering = Unit,
            output = EmittedOutput(Unit)
        )

    val failure = assertFailsWith<IllegalStateException> {
      tester.expectWorkflow(
          workflow::class, rendering = Unit, output = EmittedOutput(Unit)
      )
    }

    val failureMessage = failure.message!!
    assertTrue(failureMessage.startsWith("Expected only one child to emit an output:"))
    assertEquals(
        3, failureMessage.lines().size,
        "Expected error message to have 3 lines, but was ${failureMessage.lines().size}"
    )
    assertEquals(2, failureMessage.lines().count { "ExpectedWorkflow" in it })
  }

  @Test fun `expectWorkflow with output throws when already expecting worker output`() {
    // Don't need an implementation, the test should fail before even calling render.
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true }, output = EmittedOutput(Unit))

    val failure = assertFailsWith<IllegalStateException> {
      tester.expectWorkflow(
          workflow::class, rendering = Unit, output = EmittedOutput(Unit)
      )
    }

    val failureMessage = failure.message!!
    assertTrue(failureMessage.startsWith("Expected only one child to emit an output:"))
    assertEquals(
        3, failureMessage.lines().size,
        "Expected error message to have 3 lines, but was ${failureMessage.lines().size}"
    )
    assertEquals(1, failureMessage.lines().count { "ExpectedWorker" in it })
    assertEquals(1, failureMessage.lines().count { "ExpectedWorkflow" in it })
  }

  @Test fun `expectWorkflow without output doesn't throw when already expecting output`() {
    // Don't need an implementation, the test should fail before even calling render.
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(
            OutputWhateverChild::class, rendering = Unit,
            output = EmittedOutput(Unit)
        )

    // Doesn't throw.
    tester.expectWorkflow(workflow::class, rendering = Unit)
  }

  @Test fun `expectWorker with output throws when already expecting worker output`() {
    // Don't need an implementation, the test should fail before even calling render.
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {}
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true }, output = EmittedOutput(Unit))

    val failure = assertFailsWith<IllegalStateException> {
      tester.expectWorker(matchesWhen = { true }, output = EmittedOutput(Unit))
    }

    val failureMessage = failure.message!!
    assertTrue(failureMessage.startsWith("Expected only one child to emit an output:"))
    assertEquals(
        3, failureMessage.lines().size,
        "Expected error message to have 3 lines, but was ${failureMessage.lines().size}"
    )
    assertEquals(2, failureMessage.lines().count { "ExpectedWorker" in it })
  }

  @Test fun `expectWorker with output throws when already expecting workflow output`() {
    // Don't need an implementation, the test should fail before even calling render.
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(
            workflow::class, rendering = Unit,
            output = EmittedOutput(Unit)
        )

    val failure = assertFailsWith<IllegalStateException> {
      tester.expectWorker(matchesWhen = { true }, output = EmittedOutput(Unit))
    }

    val failureMessage = failure.message!!
    assertTrue(failureMessage.startsWith("Expected only one child to emit an output:"))
    assertEquals(
        3, failureMessage.lines().size,
        "Expected error message to have 3 lines, but was ${failureMessage.lines().size}"
    )
    assertEquals(1, failureMessage.lines().count { "ExpectedWorker" in it })
    assertEquals(1, failureMessage.lines().count { "ExpectedWorkflow" in it })
  }

  @Test fun `expectWorker without output doesn't throw when already expecting output`() {
    // Don't need an implementation, the test should fail before even calling render.
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {}
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true }, output = EmittedOutput(Unit))

    // Doesn't throw.
    tester.expectWorker(matchesWhen = { true })
  }

  @Test fun `expectWorker taking Worker matches`() {
    val worker = object : Worker<String> {
      override fun doesSameWorkAs(otherWorker: Worker<*>) = otherWorker === this
      override fun run() = emptyFlow<Nothing>()
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker) { noAction() }
    }

    workflow.renderTester(Unit)
        .expectWorker(worker)
        .render()
  }

  @Test fun `expectWorker taking Worker doesn't match`() {
    val worker1 = object : Worker<String> {
      override fun doesSameWorkAs(otherWorker: Worker<*>) = otherWorker === this
      override fun toString(): String = "Worker1"
      override fun run() = emptyFlow<Nothing>()
    }
    val worker2 = object : Worker<String> {
      override fun doesSameWorkAs(otherWorker: Worker<*>) = otherWorker === this
      override fun toString(): String = "Worker2"
      override fun run() = emptyFlow<Nothing>()
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker1) { noAction() }
    }
    val tester = workflow.renderTester(Unit)
        .expectWorker(worker2)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(error.message!!.startsWith("Expected 1 more workflows or workers to be ran:"))
  }

  @Test fun `sending to sink throws when called multiple times`() {
    class TestAction(private val name: String) : WorkflowAction<Unit, Nothing> {
      override fun Updater<Unit, Nothing>.apply() {}
      override fun toString(): String = "TestAction($name)"
    }

    val workflow = Workflow.stateful<Unit, Nothing, Sink<TestAction>>(
        initialState = Unit,
        render = { actionSink.contraMap { it } }
    )
    val action1 = TestAction("1")
    val action2 = TestAction("2")

    workflow.renderTester(Unit)
        .render { sink ->
          sink.send(action1)

          val error = assertFailsWith<IllegalStateException> {
            sink.send(action2)
          }
          assertEquals(
              "Tried to send action to sink after another action was already processed:\n" +
                  "  processed action=$action1\n" +
                  "  attempted action=$action2",
              error.message
          )
        }
  }

  @Test fun `sending to sink throws when child output expected`() {
    class TestAction : WorkflowAction<Unit, Nothing> {
      override fun Updater<Unit, Nothing>.apply() {}
    }

    val workflow = Workflow.stateful<Unit, Nothing, Sink<TestAction>>(
        initialState = Unit,
        render = {
          // Need to satisfy the expectation.
          runningWorker(Worker.finished<Unit>()) { noAction() }
          return@stateful actionSink.contraMap { it }
        }
    )

    workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true }, output = EmittedOutput(Unit))
        .render { sink ->
          val error = assertFailsWith<IllegalStateException> {
            sink.send(TestAction())
          }
          assertTrue(error.message!!.startsWith("Expected only one child to emit an output:"))
        }
  }

  @Test fun `failures from render block escape`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { }
    val tester = workflow.renderTester(Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render {
        throw AssertionError("expected failure")
      }
    }
    assertEquals("expected failure", error.message)
  }

  @Test fun `renderChild throws when none expected`() {
    val child = Workflow.stateless<Unit, Nothing, Unit> { }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child)
    }
    val tester = workflow.renderTester(Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertEquals(
        "Tried to render unexpected child workflow ${child::class.java.name}.",
        error.message
    )
  }

  @Test fun `renderChild throws when no expectations match`() {
    val child = Workflow.stateless<Unit, Nothing, Unit> { }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child)
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(OutputNothingChild::class, rendering = Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertEquals(
        "Tried to render unexpected child workflow ${child::class.java.name}.",
        error.message
    )
  }

  @Test fun `renderChild with key throws when none expected`() {
    val child = Workflow.stateless<Unit, Nothing, Unit> { }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child, key = "key")
    }
    val tester = workflow.renderTester(Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertEquals(
        "Tried to render unexpected child workflow ${child::class.java.name} " +
            "with key \"key\".",
        error.message
    )
  }

  @Test fun `renderChild with key throws when no expectations match`() {
    val child = Workflow.stateless<Unit, Nothing, Unit> { }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child, key = "key")
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(OutputNothingChild::class, rendering = Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertEquals(
        "Tried to render unexpected child workflow ${child::class.java.name} " +
            "with key \"key\".",
        error.message
    )
  }

  @Test fun `renderChild with key throws when key doesn't match`() {
    val child = Workflow.stateless<Unit, Nothing, Unit> { }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child, key = "key")
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(OutputNothingChild::class, rendering = Unit, key = "wrong key")

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertEquals(
        "Tried to render unexpected child workflow ${child::class.java.name} " +
            "with key \"key\".",
        error.message
    )
  }

  @Test fun `renderChild throws when multiple expectations match`() {
    class Child : OutputNothingChild, StatelessWorkflow<Unit, Nothing, Unit>() {
      override fun render(
        props: Unit,
        context: RenderContext<Nothing, Nothing>
      ) {
        // Nothing to do.
      }
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(Child())
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(OutputNothingChild::class, rendering = Unit)
        .expectWorkflow(Child::class, rendering = Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(
        error.message!!.startsWith(
            "Multiple expectations matched child workflow ${Child::class.java.name}:"
        ),
        "Wrong message: ${error.message}"
    )
  }

  @Test fun `runningWorker doesn't throw when none expected`() {
    val worker = object : Worker<Nothing> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = true
      override fun run(): Flow<Nothing> = emptyFlow()
      override fun toString(): String = "TestWorker"
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker)
    }
    val tester = workflow.renderTester(Unit)
    tester.render()
  }

  @Test fun `runningWorker throws when expectation doesn't match`() {
    val worker = object : Worker<Nothing> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = true
      override fun run(): Flow<Nothing> = emptyFlow()
      override fun toString(): String = "TestWorker"
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker)
    }
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { false })

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }

    assertTrue(error.message!!.startsWith("Expected 1 more workflows or workers to be ran:"))
  }

  @Test fun `runningWorker with key does not throw when none expected`() {
    val worker = object : Worker<Nothing> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = true
      override fun run(): Flow<Nothing> = emptyFlow()
      override fun toString(): String = "TestWorker"
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker, key = "key")
    }
    val tester = workflow.renderTester(Unit)

    tester.render()
  }

  @Test fun `runningWorker with key throws when no key expected`() {
    val worker = object : Worker<Nothing> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = true
      override fun run(): Flow<Nothing> = emptyFlow()
      override fun toString(): String = "TestWorker"
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker, key = "key")
    }
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true })

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(error.message!!.startsWith("Expected 1 more workflows or workers to be ran:"))
  }

  @Test fun `runningWorker with key throws when wrong key expected`() {
    val worker = object : Worker<Nothing> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = true
      override fun run(): Flow<Nothing> = emptyFlow()
      override fun toString(): String = "TestWorker"
    }

    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker, key = "key")
    }
    val tester = workflow.renderTester(Unit)
        .expectWorker(
            matchesWhen = { true },
            key = "wrong key"
        )

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(error.message!!.startsWith("Expected 1 more workflows or workers to be ran:"))
  }

  @Test fun `runningWorker throws when multiple expectations match`() {
    val worker = object : Worker<Nothing> {
      override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean = true
      override fun run(): Flow<Nothing> = emptyFlow()
      override fun toString(): String = "TestWorker"
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker)
    }
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true })
        .expectWorker(matchesWhen = { true })

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(error.message!!.startsWith("Multiple expectations matched worker TestWorker:"))
  }

  @Test fun `render throws when unconsumed workflow`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      // Do nothing.
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(OutputNothingChild::class, rendering = Unit)

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(error.message!!.startsWith("Expected 1 more workflows or workers to be ran:"))
  }

  @Test fun `render throws when unconsumed worker`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      // Do nothing.
    }
    val tester = workflow.renderTester(Unit)
        .expectWorker(matchesWhen = { true })

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertTrue(error.message!!.startsWith("Expected 1 more workflows or workers to be ran:"))
  }

  @Test fun `expectWorkflow matches on workflow supertype`() {
    val child = object : OutputNothingChild, StatelessWorkflow<Unit, Nothing, Unit>() {
      override fun render(
        props: Unit,
        context: RenderContext<Nothing, Nothing>
      ) {
        // Do nothing.
      }
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child)
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(OutputNothingChild::class, rendering = Unit)

    tester.render()
  }

  @Test fun `assertProps failure fails test`() {
    val child = Workflow.stateless<String, Nothing, Unit> {}
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child, "wrong props")
    }
    val tester = workflow.renderTester(Unit)
        .expectWorkflow(
            workflowType = child::class,
            rendering = Unit,
            assertProps = { props -> throw AssertionError("bad props: $props") }
        )

    val error = assertFailsWith<AssertionError> {
      tester.render()
    }
    assertEquals("bad props: wrong props", error.message)
  }

  private class TestAction(val name: String) : WorkflowAction<Nothing, Nothing> {
    override fun Updater<Nothing, Nothing>.apply() {}
    override fun toString(): String = "TestAction($name)"
  }

  @Test fun `verifyAction failure fails test`() {
    val workflow = Workflow.stateless<Unit, Nothing, Sink<TestAction>> {
      actionSink.contraMap { it }
    }
    val testResult = workflow.renderTester(Unit)
        .render { it.send(TestAction("noop")) }

    val error = assertFailsWith<AssertionError> {
      testResult.verifyAction { throw AssertionError("action failed") }
    }
    assertEquals("action failed", error.message)
  }

  @Test fun `verifyAction verifies workflow output`() {
    val child = Workflow.stateless<Unit, String, Unit> {}
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderChild(child) { TestAction(it) }
    }
    val testResult = workflow.renderTester(Unit)
        .expectWorkflow(
            workflowType = child::class,
            rendering = Unit,
            output = EmittedOutput("output")
        )
        .render()

    testResult.verifyAction {
      assertTrue(it is TestAction)
      assertEquals("output", it.name)
    }
  }

  @Test fun `verifyAction verifies worker output`() {
    val worker = Worker.finished<String>()
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(worker) { TestAction(it) }
    }
    val testResult = workflow.renderTester(Unit)
        .expectWorker(
            matchesWhen = { true },
            output = EmittedOutput("output")
        )
        .render()

    testResult.verifyAction {
      assertTrue(it is TestAction)
      assertEquals("output", it.name)
    }
  }

  @Test fun `verifyAction verifies sink send`() {
    val workflow = Workflow.stateless<Unit, Nothing, Sink<TestAction>> {
      actionSink.contraMap { it }
    }
    val testResult = workflow.renderTester(Unit)
        .render { sink ->
          sink.send(TestAction("event"))
        }

    testResult.verifyAction {
      assertTrue(it is TestAction)
      assertEquals("event", it.name)
    }
  }

  @Test fun `verifyAction and verifyActionResult pass when no action processed`() {
    val workflow = Workflow.stateless<Unit, Nothing, Sink<TestAction>> {
      actionSink.contraMap { it }
    }
    val testResult = workflow.renderTester(Unit)
        .render {
          // Don't send to sink!
        }

    testResult.verifyAction { assertEquals(noAction(), it) }
    testResult.verifyActionResult { newState, output ->
      assertSame(Unit, newState)
      assertNull(output)
    }
  }

  @Test fun `verifyActionResult works`() {
    class TestAction : WorkflowAction<String, String> {
      override fun Updater<String, String>.apply() {
        nextState = "new state"
        setOutput("output")
      }
    }

    val workflow = Workflow.stateful<Unit, String, String, Sink<TestAction>>(
        initialState = { "initial" },
        render = { _, _ -> actionSink.contraMap { it } }
    )
    val testResult = workflow.renderTester(Unit)
        .render { sink ->
          sink.send(TestAction())
        }

    testResult.verifyActionResult { state, output ->
      assertEquals("new state", state)
      assertEquals("output", output)
    }
  }

  @Test fun `render is executed multiple times`() {
    var renderCount = 0
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { renderCount++ }

    workflow.renderTester(Unit)
        .render()

    assertEquals(2, renderCount)
  }
}
