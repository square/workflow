package com.squareup.viewbuilder

interface EventHandlingScreen<out D : Any, in E : Any> {
  val data: D
  val onEvent: (E) -> Unit

  companion object {
    fun <E> ignoreEvents(): (E) -> Unit {
      return { Unit }
    }
  }
}
