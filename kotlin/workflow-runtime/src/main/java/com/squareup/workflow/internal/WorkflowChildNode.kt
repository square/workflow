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

import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.internal.InlineLinkedList.InlineListNode

/**
 * Representation of a child workflow that has been rendered by another workflow.
 *
 * Associates the child's [WorkflowNode] (which includes the key passed to `renderChild`) with the
 * output handler function that was passed to `renderChild`.
 */
/* ktlint-disable parameter-list-wrapping */
internal class WorkflowChildNode<
    ChildPropsT,
    ChildOutputT : Any,
    ParentStateT,
    ParentOutputT : Any
    >(
  val workflow: Workflow<*, ChildOutputT, *>,
  private var handler: (ChildOutputT) -> WorkflowAction<ParentStateT, ParentOutputT>,
  val workflowNode: WorkflowNode<ChildPropsT, *, ChildOutputT, *>
) : InlineListNode<WorkflowChildNode<*, *, *, *>> {
/* ktlint-enable parameter-list-wrapping */

  override var nextListNode: WorkflowChildNode<*, *, *, *>? = null

  /** The [WorkflowNode]'s [WorkflowId]. */
  val id get() = workflowNode.id

  /**
   * Returns true if this child has the same type as [otherWorkflow] and key as [key].
   */
  fun matches(
    otherWorkflow: Workflow<*, *, *>,
    key: String
  ): Boolean = id.let { (myClass, myKey) -> myClass == otherWorkflow::class && myKey == key }

  /**
   * Updates the handler function that will be invoked by [acceptChildOutput].
   */
  fun <CO, S, O : Any> setHandler(newHandler: (CO) -> WorkflowAction<S, O>) {
    @Suppress("UNCHECKED_CAST")
    handler = newHandler as (ChildOutputT) -> WorkflowAction<ParentStateT, ParentOutputT>
  }

  /**
   * Wrapper around [WorkflowNode.render] that allows calling it with erased types.
   */
  fun <R> render(
    workflow: StatefulWorkflow<*, *, *, *>,
    props: Any?
  ): R {
    @Suppress("UNCHECKED_CAST")
    return workflowNode.render(
        workflow as StatefulWorkflow<ChildPropsT, out Any?, ChildOutputT, Nothing>,
        props as ChildPropsT
    ) as R
  }

  /**
   * Wrapper around [handler] that allows calling it with erased types.
   */
  @Suppress("UNCHECKED_CAST")
  fun acceptChildOutput(output: Any): WorkflowAction<ParentStateT, ParentOutputT> =
    handler(output as ChildOutputT)
}
