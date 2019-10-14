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
@file:Suppress("DEPRECATION", "OverridingDeprecatedMember")

package com.squareup.workflow.testing

import com.squareup.workflow.EventHandler
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.internal.Behavior
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.RealRenderContext
import com.squareup.workflow.internal.RealRenderContext.Renderer
import com.squareup.workflow.internal.WorkflowId

/**
 * Wraps a [RealRenderContext] and asserts that workflows and workers are of the correct mock type.
 */
internal class TestOnlyRenderContext<S, O : Any> : RenderContext<S, O>, Renderer<S, O> {

  private val realContext = RealRenderContext(this)

  override fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<S, O>
  ): EventHandler<EventT> = realContext.onEvent(handler)

  override fun <A : WorkflowAction<S, O>> makeActionSink(): Sink<A> = realContext.makeActionSink()

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<S, O>
  ): ChildRenderingT = realContext.renderChild(child, props, key, handler)

  override fun <T> runningWorker(
    worker: Worker<T>,
    key: String,
    handler: (T) -> WorkflowAction<S, O>
  ) = realContext.runningWorker(worker, key, handler)

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> render(
    case: WorkflowOutputCase<ChildPropsT, ChildOutputT, S, O>,
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    id: WorkflowId<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT
  ): ChildRenderingT {
    @Suppress("UNCHECKED_CAST")
    val childStatefulWorkflow =
      child.asStatefulWorkflow() as StatefulWorkflow<ChildPropsT, Any?, ChildOutputT, ChildRenderingT>
    val childInitialState = childStatefulWorkflow.initialState(props, null)
    // Allow the workflow-under-test to *render* children, but those children must not try to
    // use the RenderContext themselves.
    return childStatefulWorkflow.render(props, childInitialState, NoopRenderContext)
  }

  fun buildBehavior(): Behavior<S, O> = realContext.buildBehavior()
}

private object NoopRenderContext : RenderContext<Any?, Any> {
  override fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<Any?, Any>
  ): EventHandler<EventT> {
    throw UnsupportedOperationException()
  }

  override fun <A : WorkflowAction<Any?, Any>> makeActionSink(): Sink<A> {
    throw UnsupportedOperationException()
  }

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<Any?, Any>
  ): ChildRenderingT {
    throw UnsupportedOperationException()
  }

  override fun <T> runningWorker(
    worker: Worker<T>,
    key: String,
    handler: (T) -> WorkflowAction<Any?, Any>
  ) {
    throw UnsupportedOperationException()
  }
}
