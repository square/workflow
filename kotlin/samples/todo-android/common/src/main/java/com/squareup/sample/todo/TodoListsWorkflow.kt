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
package com.squareup.sample.todo

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.makeEventSink

class TodoListsWorkflow : StatelessWorkflow<List<TodoList>, Int, TodoListsScreen>() {
  override fun render(
    props: List<TodoList>,
    context: RenderContext<Nothing, Int>
  ): TodoListsScreen {
    // A sink that emits the given index as the output of this workflow.
    val sink: Sink<Int> = context.makeEventSink { index: Int -> setOutput(index) }

    return TodoListsScreen(lists = props, onRowClicked = sink::send)
  }
}
