@file:JvmName("WorkflowInputs")

package com.squareup.workflow

import io.reactivex.functions.Consumer

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Wouldn't you rather be using Kotlin?")
fun <E> forJava(handler: Consumer<E>): WorkflowInput<E> =
  WorkflowInput { handler.accept(it) }
