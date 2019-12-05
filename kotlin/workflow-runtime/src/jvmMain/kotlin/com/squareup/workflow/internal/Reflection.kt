package com.squareup.workflow.internal

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
actual inline fun <reified T : Any> kClassForName(typeName:String): KClass<out T> =
    Class.forName(typeName).kotlin as KClass<out T>


actual fun <T: Any> KClass<T>.name(): String = java.name
