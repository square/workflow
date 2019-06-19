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
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow

data class TodoList(
  val title: String,
  val rows: List<TodoRow> = emptyList()
)

data class TodoRow(
  val text: String,
  val done: Boolean = false
)

class TodoEditorWorkflow : StatefulWorkflow<Unit, TodoList, Nothing, TodoList>() {

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): TodoList = TodoList(title = "Groceries", rows = listOf(TodoRow("Potatoes")))

  override fun render(
    input: Unit,
    state: TodoList,
    context: RenderContext<TodoList, Nothing>
  ): TodoList {
    return state.copy(rows = state.rows + TodoRow(text = ""))
  }

  override fun snapshotState(state: TodoList): Snapshot = Snapshot.EMPTY
}
