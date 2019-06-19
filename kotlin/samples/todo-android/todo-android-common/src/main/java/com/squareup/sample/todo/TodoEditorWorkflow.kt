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

import com.squareup.sample.todo.TodoEditorState.Loaded
import com.squareup.sample.todo.TodoEditorState.Loading
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.asWorker
import com.squareup.workflow.onWorkerOutput
import kotlinx.coroutines.ExperimentalCoroutinesApi

@UseExperimental(VeryExperimentalWorkflow::class, ExperimentalCoroutinesApi::class)
class TodoEditorWorkflow(
  repository: TodoRepository,
  id: String = "1"
) : StatefulWorkflow<Unit, TodoEditorState, Nothing, TodoEditorState>() {

  private val todoWorker = repository.getTodo(id)
      .asWorker()

  override fun initialState(
    input: Unit,
    snapshot: Snapshot?
  ): TodoEditorState = Loading

  override fun render(
    input: Unit,
    state: TodoEditorState,
    context: RenderContext<TodoEditorState, Nothing>
  ): TodoEditorState {

    return when (state) {
      Loading -> {
        context.onWorkerOutput(todoWorker) {
          println("Loaded: $it")
          enterState(Loaded(it, saved = true))
        }
        state
      }
      is Loaded -> state
    }
  }

  override fun snapshotState(state: TodoEditorState): Snapshot = Snapshot.EMPTY
}
