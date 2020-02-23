/*
 * Copyright 2020 Square Inc.
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
package com.squareup.workflow.ui.compose.tooling

import androidx.compose.Composable
import androidx.compose.Immutable
import com.squareup.workflow.Sink
import com.squareup.workflow.compose.ComposeWorkflow
import com.squareup.workflow.ui.ViewBinding

/**
 * Draws this [ComposeWorkflow] using a special preview `ViewRegistry`.
 *
 * The sink passed to [ComposeWorkflow.render] will be a no-op implementation, since previews can't
 * process input.
 *
 * Use inside `@Preview` Composable functions.
 */
@Composable fun <PropsT, OutputT : Any> ComposeWorkflow<PropsT, OutputT>.preview(
  props: PropsT,
  stubBinding: ViewBinding<Any> = PreviewStubViewBinding
) {
  val containerHints = PreviewContainerHints(stubBinding)
  render(props, NoopSink, containerHints)
}

@Immutable
private object NoopSink : Sink<Any?> {
  override fun send(value: Any?) = Unit
}
