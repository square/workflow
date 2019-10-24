package com.squareup.workflow.ui

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class Hints private constructor(
  private val map: Map<HintKey<*>, Any>
) {
  constructor() : this(emptyMap())

  operator fun <T : Any> get(key: HintKey<T>): T = map[key] as? T ?: key.default

  operator fun <T : Any> plus(pair: Pair<HintKey<T>, T>): Hints {
    return Hints(map + pair)
  }

  operator fun plus(other: Hints): Hints = Hints(map + other.map)

  override fun toString(): String {
    return "Hints($map)"
  }

  override fun equals(other: Any?) = (other as? Hints)?.let { it.map == map } ?: false

  override fun hashCode() = map.hashCode()
}

abstract class HintKey<T : Any>(
  private val type: KClass<T>
) {
  abstract val default: T

  final override fun equals(other: Any?) = when {
    this === other -> true
    other != null && this::class != other::class -> false
    else -> type == (other as HintKey<*>).type
  }

  final override fun hashCode() = type.hashCode()

  override fun toString(): String {
    return "Hint($type)"
  }
}
