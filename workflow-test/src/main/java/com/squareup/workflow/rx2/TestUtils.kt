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
@file:JvmName("TestUtils")

package com.squareup.workflow.rx2

import com.squareup.workflow.WorkflowPool
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
  val observer = workflow.result.test() as TestObserver<O>
  workflow.sendEvent(event)
  observer.assertValues(output)
  observer.assertComplete()
}
