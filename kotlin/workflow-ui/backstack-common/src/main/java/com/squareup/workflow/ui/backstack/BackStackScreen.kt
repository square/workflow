/*
 * Copyright 2018 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.ui.backstack

/**
 * Represents an active screen ([top]), and a set of previously visited
 * screens to which we may return ([backStack]). By rendering the entire
 * history we allow the UI to do things like maintain cached view state,
 * implement drag-back gestures without waiting for the workflow, etc.
 *
 * Effectively a list that can never be empty.
 *
 * @param bottom the bottom-most entry in the stack
 * @param rest the rest of the stack, empty by default
 */
class BackStackScreen<StackedT : Any>(
  bottom: StackedT,
  rest: List<StackedT>
) {
  /**
   * Creates a screen with elements listed from the [bottom] to the top.
   */
  constructor(
    bottom: StackedT,
    vararg rest: StackedT
  ) : this(bottom, rest.toList())

  val frames: List<StackedT> = listOf(bottom) + rest

  /**
   * The active screen.
   */
  val top: StackedT = frames.last()

  /**
   * Screens to which we may return.
   */
  val backStack: List<StackedT> = frames.subList(0, frames.size - 1)

  operator fun get(index: Int): StackedT = frames[index]

  operator fun plus(other: BackStackScreen<StackedT>?): BackStackScreen<StackedT> {
    return if (other == null) this
    else BackStackScreen(frames[0], frames.subList(1, frames.size) + other.frames)
  }

  fun <R : Any> map(transform: (StackedT) -> R): BackStackScreen<R> {
    return frames.map(transform)
        .toBackStackScreen()
  }

  fun <R : Any> mapIndexed(transform: (index: Int, StackedT) -> R): BackStackScreen<R> {
    return frames.mapIndexed(transform)
        .toBackStackScreen()
  }

  override fun equals(other: Any?): Boolean {
    return (other as? BackStackScreen<*>)?.frames == frames
  }

  override fun hashCode(): Int {
    return frames.hashCode()
  }

  override fun toString(): String {
    return "${this::class.java.simpleName}($frames)"
  }
}

fun <T : Any> List<T>.toBackStackScreenOrNull(): BackStackScreen<T>? = when {
  isEmpty() -> null
  else -> toBackStackScreen()
}

fun <T : Any> List<T>.toBackStackScreen(): BackStackScreen<T> {
  require(isNotEmpty())
  return BackStackScreen(first(), subList(1, size))
}
