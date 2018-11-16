package com.squareup.reactor

/**
 * Wraps exceptions thrown by [Reactor.onReact] or the Singles emitted from it.
 *
 * The message includes the name of the [Reactor] class and the result of calling [toString] on the
 * state of the `Reactor` at the time the exception was thrown.
 */
class ReactorException(
  cause: Throwable,
  val reactor: CoroutineReactor<*, *, *>,
  val reactorState: Any
) : RuntimeException(cause) {

  override val message: String
    get() = "Reactor $reactor @ $reactorState " +
        "threw ${cause!!.javaClass.simpleName}: ${cause.message}"
}
