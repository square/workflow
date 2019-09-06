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
package com.squareup.sample.mainactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.squareup.sample.todo.TodoListsAppWorkflow
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowRunner
import com.squareup.workflow.ui.setContentWorkflow
import com.squareup.workflow.ui.workflowOnBackPressed

class MainActivity : AppCompatActivity() {

  private lateinit var rootWorkflow: TodoListsAppWorkflow
  private lateinit var workflowRunner: WorkflowRunner<*>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // TODO: https://github.com/square/workflow/issues/603 Remove use of deprecated property.
    @Suppress("DEPRECATION")
    rootWorkflow = lastCustomNonConfigurationInstance as? TodoListsAppWorkflow
        ?: TodoListsAppWorkflow()

    workflowRunner = setContentWorkflow(savedInstanceState) {
      WorkflowRunner.Config(rootWorkflow, viewRegistry)
    }
  }

  override fun onBackPressed() {
    if (!workflowOnBackPressed()) super.onBackPressed()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    workflowRunner.onSaveInstanceState(outState)
  }

  override fun onRetainCustomNonConfigurationInstance(): Any = rootWorkflow

  private companion object {
    val viewRegistry = ViewRegistry(TodoEditorLayoutRunner) + TodoListsLayoutRunner
  }
}
