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
package com.squareup.sample.container.masterdetail

/**
 * Informs [MasterDetailAware] renderings whether they're children of a [MasterDetailScreen],
 * and if so in what configuration.
 */
enum class MasterDetailConfig {
  /**
   * Rendering is in [MasterDetailScreen.masterRendering], running in split screen
   * configuration.
   */
  Master,

  /**
   * Rendering is in [MasterDetailScreen.detailRendering], running in split screen
   * configuration.
   */
  Detail,

  /**
   * Rendering is the only visible child in a [MasterDetailScreen] running in
   * single screen configuration.
   */
  Only
}

/**
 * Implemented by rendering types to allow them to customize appearance / behavior
 * based on a [MasterDetailScreen] parent's [configuration][MasterDetailConfig].
 */
interface MasterDetailAware<T : MasterDetailAware<T>> {
  /**
   * The configuration of a possible [MasterDetailScreen] parent rendering. Should default to null,
   * and be set only via [withMasterDetailConfig].
   */
  val masterDetailConfig: MasterDetailConfig?

  /**
   * Creates a copy of the receiver with [masterDetailConfig] set to the given [config].
   * A container view that displays a [MasterDetailScreen] should use this method to
   * transform each visible child before rendering it.
   */
  fun withMasterDetailConfig(config: MasterDetailConfig): T

  companion object {
    /**
     * Convenience allowing [MasterDetailScreen] containers to call
     * [withMasterDetailConfig] on conforming children before displaying them.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> makeAware(
      maybeAware: T,
      config: MasterDetailConfig
    ): T = when (maybeAware) {
      is MasterDetailAware<*> -> maybeAware.withMasterDetailConfig(config) as T
      else -> maybeAware
    }
  }
}
