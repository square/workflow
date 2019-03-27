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
package com.squareup.workflow

import com.squareup.workflow.WorkflowAction.Companion.emitOutput

/**
 * Wraps this `Workflow` in one that will apply [transform] to its renderings.
 */
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
fun <InputT : Any, OutputT : Any, FromRenderingT : Any, ToRenderingT : Any>
    Workflow<InputT, OutputT, FromRenderingT>.mapRendering(
      transform: (FromRenderingT) -> ToRenderingT
    ): Workflow<InputT, OutputT, ToRenderingT> = StatelessWorkflow { input, context ->
// @formatter:on
  context.compose(this@mapRendering, input) { emitOutput(it) }
      .let(transform)
}
