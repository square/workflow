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
package com.squareup.sample.poetry

import com.squareup.sample.poetry.model.Poem
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.makeEventSink

/**
 * Given a [Poem], renders a list of its [initialStanzas][Poem.initialStanzas].
 *
 * Output is the index of a clicked stanza, or -1 on exit.
 */
object StanzaListWorkflow : StatelessWorkflow<Poem, Int, StanzaListRendering>() {

  override fun render(
    props: Poem,
    context: RenderContext<Nothing, Int>
  ): StanzaListRendering {
    // A sink that emits the given index as the result of this workflow.
    val sink = context.makeEventSink { index: Int -> setOutput(index) }

    return StanzaListRendering(
        title = props.title,
        subtitle = props.poet.fullName,
        firstLines = props.initialStanzas,
        onStanzaSelected = sink::send,
        onExit = { sink.send(-1) }
    )
  }
}

data class StanzaListRendering(
  val title: String,
  val subtitle: String,
  val firstLines: List<String>,
  val onStanzaSelected: (Int) -> Unit,
  val onExit: () -> Unit,
  val selection: Int = -1
)
