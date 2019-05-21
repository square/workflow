/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy

/**
 * Accepts input [events][E] for a `Workflow` implementation.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
interface WorkflowInput<in E> {
  /**
   * Sends this event to a `Workflow` to be processed.
   */
  fun sendEvent(event: E)

  /**
   * For use by `Workflow` implementations that accept no input.
   */
  object ReadOnly : WorkflowInput<Nothing> {
    override fun sendEvent(event: Nothing) = Unit
  }

  companion object {
    fun <E> disabled(): WorkflowInput<E> = NoOp.adaptEvents<E, Unit> { }
  }
}

/**
 * Convenience for presenting a lambda as a [WorkflowInput].
 */
@Suppress("FunctionName")
@Deprecated("Use com.squareup.workflow.Workflow")
fun <E> WorkflowInput(handler: (E) -> Unit): WorkflowInput<E> {
  return object : WorkflowInput<E> {
    override fun sendEvent(event: E) = handler(event)
  }
}

/**
 * [Transforms][https://stackoverflow.com/questions/15457015/explain-contramap]
 * the receiving [WorkflowInput]<[E1]> into a [WorkflowInput]<[E2]>.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
fun <E2, E1> WorkflowInput<E1>.adaptEvents(transform: (E2) -> E1): WorkflowInput<E2> {
  return WorkflowInput { event -> sendEvent(transform(event)) }
}

internal object NoOp : WorkflowInput<Any> {
  override fun sendEvent(event: Any) {
  }
}
