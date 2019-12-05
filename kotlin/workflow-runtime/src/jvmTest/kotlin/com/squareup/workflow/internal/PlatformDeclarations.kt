package com.squareup.workflow.internal

import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T
    = kotlinx.coroutines.runBlocking(context,block)

actual val <T : Any> KClass<T>.declaredMemberFunctions: Collection<KFunction<*>>
  get() = declaredMemberFunctions

actual typealias AtomicInteger = AtomicInteger