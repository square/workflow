/*
 * Copyright 2018 Square Inc.
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
@file:JvmName("Assertions")
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.test

import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.rx2.Reactor
import com.squareup.workflow.legacy.rx2.result
import com.squareup.workflow.legacy.rx2.state
import io.reactivex.observers.TestObserver

/**
 * Assert that when the given [Reactor] is in [fromState], it will enter [toState] when it receives [event].
 */
fun <S : Any, E : Any, O : Any> Reactor<S, E, O>.assertTransition(
  fromState: S,
  event: E,
  toState: S
) {
  val workflow = launch(fromState, WorkflowPool())
  @Suppress("UNCHECKED_CAST")
  val observer = workflow.state.test() as TestObserver<S>
  workflow.sendEvent(event)
  observer.assertValues(fromState, toState)
}

/**
 * Assert that when the given [Reactor] is in [fromState], it will finish with [output] when it receives [event].
 */
fun <S : Any, E : Any, O : Any> Reactor<S, E, O>.assertFinish(
  fromState: S,
  event: E,
  output: O
) {
  val workflow = launch(fromState, WorkflowPool())
  @Suppress("UNCHECKED_CAST")
  val observer = workflow.result.test() as TestObserver<O>
  workflow.sendEvent(event)
  observer.assertValues(output)
  observer.assertComplete()
}
