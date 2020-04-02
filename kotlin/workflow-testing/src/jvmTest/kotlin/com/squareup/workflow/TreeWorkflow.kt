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

import com.squareup.workflow.TreeWorkflow.Rendering

/**
 * A [Workflow] that has a simple string state and can be configured with children at construction.
 */
internal class TreeWorkflow(
  private val name: String,
  private vararg val children: TreeWorkflow
) : StatefulWorkflow<String, String, Nothing, Rendering>() {

  class Rendering(
    val data: String,
    val setData: (String) -> Unit,
    val children: Map<String, Rendering> = emptyMap()
  ) {
    /**
     * Walk this rendering's tree of children to find a rendering.
     *
     * The first argument is looked up in this.[children], the 2nd part, if any, is looked up
     * in that rendering's children, and so on.
     */
    operator fun get(vararg path: String): Rendering {
      return path.fold(this) { node, pathPart ->
        node.children.getValue(pathPart)
      }
    }
  }

  override fun initialState(
    props: String,
    snapshot: Snapshot?
  ): String = snapshot?.bytes?.parse {
    it.readUtf8WithLength()
  } ?: props

  override fun render(
    props: String,
    state: String,
    context: RenderContext<String, Nothing>
  ): Rendering {
    val childRenderings = children
        .mapIndexed { index, child ->
          val childRendering = context.renderChild(child, "$props[$index]", child.name)
          Pair(child.name, childRendering)
        }
        .toMap()

    return Rendering(
        data = "$name:$state",
        setData = { context.actionSink.send(onEvent(it)) },
        children = childRenderings
    )
  }

  override fun snapshotState(state: String): Snapshot =
    Snapshot.write {
      it.writeUtf8WithLength(state)
    }

  private fun onEvent(newState: String) = action {
    nextState = newState
  }
}
