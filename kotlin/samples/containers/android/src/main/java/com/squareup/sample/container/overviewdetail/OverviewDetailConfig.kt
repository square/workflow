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
package com.squareup.sample.container.overviewdetail

import com.squareup.workflow.ui.ViewEnvironmentKey

/**
 * [com.squareup.workflow.ui.ViewEnvironment] value that informs views
 * whether they're children of a [OverviewDetailContainer], and if so
 * in what configuration.
 */
enum class OverviewDetailConfig {
  /**
   * There is no [OverviewDetailContainer] above here.
   */
  None,

  /**
   * Drawing on the overview side of a overview / detail split screen.
   */
  Overview,

  /**
   * Drawing on the detail side of a overview / detail split screen.
   */
  Detail,

  /**
   * Drawing in single screen configuration.
   */
  Single;

  companion object : ViewEnvironmentKey<OverviewDetailConfig>(OverviewDetailConfig::class) {
    override val default = None
  }
}
