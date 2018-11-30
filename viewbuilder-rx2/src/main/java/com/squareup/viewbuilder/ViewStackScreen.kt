package com.squareup.viewbuilder

import io.reactivex.Observable

data class ViewStackScreen<T : Any>(
  val wrapped: T,
  private val keyExtension: String = ""
) {
  val key = ViewStackKey(wrapped::class.java, keyExtension)
}

data class ViewStackKey<T : Any>(
  val type: Class<T>,
  val extension: String
)

fun <T : Any> Observable<out ViewStackScreen<*>>.matchingWrappedScreens(
  screen: ViewStackScreen<T>
): Observable<out T> {
  return filter { it.key == screen.key }.map {
    screen.key.type.cast(it.wrapped)
  }
}
