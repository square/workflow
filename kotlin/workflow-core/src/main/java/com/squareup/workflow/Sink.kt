package com.squareup.workflow

/**
 * An object that receives values (commonly events or [WorkflowAction]).
 * Use [RenderContext.makeSink] to create instances.
 */
interface Sink<T> {
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
