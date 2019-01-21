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
package com.squareup.sample.authgameapp

import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPoolMonitor
import com.squareup.workflow.WorkflowPoolMonitorEvent
import timber.log.Timber

class TimberWorkflowPoolMonitor : WorkflowPoolMonitor {
  override fun report(
    pool: WorkflowPool,
    event: WorkflowPoolMonitorEvent
  ) {
    Timber.tag("WorkflowPoolMonitor")
    Timber.v("WorkflowPool event:\n\t%s", event)
  }
}
