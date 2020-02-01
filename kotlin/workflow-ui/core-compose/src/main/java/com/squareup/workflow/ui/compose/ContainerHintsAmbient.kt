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
package com.squareup.workflow.ui.compose

import androidx.compose.Ambient
import androidx.compose.Composable
import androidx.compose.ambient
import com.squareup.workflow.ui.ContainerHintKey
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding

/**
 * [Ambient] that retrieves the current [ContainerHints] for a [ViewBinding].
 *
 * Access this from your `@Composable` function by calling [ambientContainerHint].
 */
val ContainerHintsAmbient: Ambient<ContainerHints> =
  Ambient.of { error("No ContainerHints ambient available.") }

/**
 * Retrieve the container hint for the given [key][ContainerHintKey] from the current
 * [ContainerHintsAmbient].
 *
 * Usage:
 *
 * ```
 * @Composable fun showFoo(foo: FooRendering) {
 *   val masterDetailConfig = ambientContainerHint(MasterDetailConfig)
 * }
 * ```
 */
@Composable
fun <T : Any> ambientContainerHint(key: ContainerHintKey<T>): T =
  ambient(ContainerHintsAmbient)[key]
