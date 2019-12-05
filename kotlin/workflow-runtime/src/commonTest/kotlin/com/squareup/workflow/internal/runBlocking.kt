package com.squareup.workflow.internal

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

expect val <T:Any> KClass<T>.declaredMemberFunctions: Collection<KFunction<*>>

expect class AtomicInteger(initialValue: Int) {
  fun getAndIncrement():Int
}