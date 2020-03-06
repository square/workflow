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
package com.squareup.workflow.ui.backstack

import com.squareup.workflow.ui.ViewEnvironmentKey
import com.squareup.workflow.ui.backstack.BackStackConfig.First
import com.squareup.workflow.ui.backstack.BackStackConfig.Other

/**
 * Informs views whether they're children of a [BackStackContainer],
 * and if so whether they're the [first frame][First] or [not][Other].
 */
enum class BackStackConfig {
  /**
   * There is no [BackStackContainer] above here.
   */
  None,

  /**
   * This rendering is the first frame in a [BackStackScreen].
   * Useful as a hint to disable "go back" behavior, or replace it with "go up" behavior.
   */
  First,

  /**
   * This rendering is in a [BackStackScreen] but is not the first frame.
   * Useful as a hint to enable "go back" behavior.
   */
  Other;

  companion object : ViewEnvironmentKey<BackStackConfig>(BackStackConfig::class) {
    override val default = None
  }
}
