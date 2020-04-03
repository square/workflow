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
package com.squareup.workflow

/**
 * An object that receives values (commonly events or [WorkflowAction]).
 * [RenderContext.actionSink] implements this interface.
 */
interface Sink<in T> {
  fun send(value: T)
}

/**
 * Generates a new sink of type [T2].
 *
 * Given a [transform] closure, the following code is functionally equivalent:
 *
 *    sink.send(transform(value))
 *
 *    sink.contraMap(transform).send(value)
 *
 *  **Trivia**: Why is this called `contraMap`?
 *     - `map` turns `Type<T>` into `Type<U>` via `(T)->U`.
 *     - `contraMap` turns `Type<T>` into `Type<U>` via `(U)->T`
 *
 * Another way to think about this is: `map` transforms a type by changing the
 * output types of its API, while `contraMap` transforms a type by changing the
 * *input* types of its API.
 */
fun <T1, T2> Sink<T1>.contraMap(transform: (T2) -> T1): Sink<T2> {
  return object : Sink<T2> {
    override fun send(value: T2) {
      this@contraMap.send(transform(value))
    }
  }
}
