package com.squareup.workflow

import io.reactivex.functions.Consumer

/**
 * Accepts input [events][E] for a `Workflow` implementation.
 */
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

  @Suppress("DeprecatedCallableAddReplaceWith")
  companion object {
    fun <E> disabled(): WorkflowInput<E> = NoOp.adaptEvents<E, Unit> { }

    @Deprecated("Wouldn't you rather be using Kotlin?")
    fun <E> forJava(handler: Consumer<E>): WorkflowInput<E> = WorkflowInput { handler.accept(it) }
  }
}

/**
 * Convenience for presenting a lambda as a [WorkflowInput].
 */
@Suppress("FunctionName")
fun <E> WorkflowInput(handler: (E) -> Unit): WorkflowInput<E> {
  return object : WorkflowInput<E> {
    override fun sendEvent(event: E) = handler(event)
  }
}

/**
 * [Transforms][https://stackoverflow.com/questions/15457015/explain-contramap]
 * the receiving [WorkflowInput]<[E1]> into a [WorkflowInput]<[E2]>.
 */
fun <E2, E1> WorkflowInput<E1>.adaptEvents(transform: (E2) -> E1): WorkflowInput<E2> {
  return WorkflowInput { event -> sendEvent(transform(event)) }
}

internal object NoOp : WorkflowInput<Any> {
  override fun sendEvent(event: Any) {
  }
}
