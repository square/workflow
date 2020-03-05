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

import com.squareup.workflow.ui.ViewEnvironmentKey

/**
 * [com.squareup.workflow.ui.ViewEnvironment] value that informs views
 * whether they're children of a [MasterDetailContainer], and if so
 * in what configuration.
 */
enum class MasterDetailConfig {
  /**
   * There is no [MasterDetailContainer] above here.
   */
  None,

  /**
   * Drawing on the master side of a master / detail split screen.
   */
  Master,

  /**
   * Drawing on the detail side of a master / detail split screen.
   */
  Detail,

  /**
   * Drawing in single screen configuration.
   */
  Single;

  companion object : ViewEnvironmentKey<MasterDetailConfig>(MasterDetailConfig::class) {
    override val default = None
  }
}
