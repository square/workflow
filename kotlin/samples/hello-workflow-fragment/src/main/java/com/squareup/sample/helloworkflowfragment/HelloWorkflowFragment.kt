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
package com.squareup.sample.helloworkflowfragment

import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowFragment
import com.squareup.workflow.ui.WorkflowRunner

@UseExperimental(ExperimentalWorkflowUi::class)
class HelloWorkflowFragment : WorkflowFragment<Unit, Unit>() {
  override fun onCreateWorkflow(): WorkflowRunner.Config<Unit, Unit> {
    return WorkflowRunner.Config(HelloWorkflow, ViewRegistry(HelloFragmentLayoutRunner))
  }
}
