/*
 * Copyright 2019 Square Inc.
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
package com.squareup.workflow.ui

import com.squareup.workflow.ui.BackStackConfig.First
import com.squareup.workflow.ui.BackStackConfig.Other

/**
 * Informs [BackStackAware] renderings whether they're children of a [BackStackScreen],
 * and if so whether they're the [first frame][First] or [not][Other].
 */
enum class BackStackConfig {
  /**
   * This rendering is first frame in a [BackStackScreen]. Useful as a hint to disable
   * "go back" behavior.
   */
  First,

  /**
   * This rendering in a [BackStackScreen] and is not the first rendering. Useful as
   * a hint to enable "go back" behavior.
   */
  Other
}

/**
 * Implemented by rendering types to allow them to customize appearance / behavior
 * based on their presence in a [BackStackScreen] parent.
 */
interface BackStackAware<T : BackStackAware<T>> {
  /**
   * The configuration of a possible [BackStackScreen] parent rendering. Should default to null,
   * and be set only via [withBackStackConfig].
   */
  val backStackConfig: BackStackConfig?

  /**
   * Creates a copy of the receiver with [backStackConfig] set to the [config].
   * A container view that displays a [BackStackScreen] should use this method
   * to transform each visible frame before rendering it.
   */
  fun withBackStackConfig(config: BackStackConfig): T

  companion object {
    /**
     * Convenience allowing [BackStackScreen] containers to call
     * [withBackStackConfig] on conforming children before displaying them.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> makeAware(
      maybeAware: T,
      config: BackStackConfig
    ): T = when (maybeAware) {
      is BackStackAware<*> -> maybeAware.withBackStackConfig(config) as T
      else -> maybeAware
    }
  }
}

val BackStackAware<*>.isInBackStack: Boolean
  get() = backStackConfig != null
