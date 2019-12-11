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
package com.squareup.sample.poetryapp

import com.squareup.sample.poetry.model.Poem
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.makeEventSink

/**
 * Renders a given ordered list of [Poem]s. Reports the index of any that are clicked.
 */
object PoemListWorkflow : StatelessWorkflow<List<Poem>, Int, PoemListRendering>() {

  override fun render(
    props: List<Poem>,
    context: RenderContext<Nothing, Int>
  ): PoemListRendering {
    // A sink that emits the given index as the result of this workflow.
    val sink = context.makeEventSink { index: Int -> setOutput(index) }

    return PoemListRendering(
        poems = props,
        onPoemSelected = sink::send
    )
  }
}

data class PoemListRendering(
  val poems: List<Poem>,
  val onPoemSelected: (Int) -> Unit,
  val selection: Int = -1
)
