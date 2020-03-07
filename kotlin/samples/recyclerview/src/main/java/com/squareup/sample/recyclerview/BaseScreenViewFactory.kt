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
package com.squareup.sample.recyclerview

import com.squareup.sample.recyclerview.AppWorkflow.BaseScreen
import com.squareup.sample.recyclerview.databinding.BaseScreenLayoutBinding
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.Asynchronous
import com.squareup.sample.recyclerview.editablelistworkflow.ListDiffMode.Synchronous
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewFactory

/**
 * Renders a [BaseScreen] from the [AppWorkflow] by showing the list in three separate
 * `RecyclerView`s at once, all bound to the same rendering.
 *
 * Each of the `RecyclerView`s uses a different [ListDiffMode] for updating its adapter.
 */
val BaseScreenViewFactory: ViewFactory<BaseScreen> =
  LayoutRunner.bind(BaseScreenLayoutBinding::inflate) { rendering, containerHints ->
    val syncHints = containerHints + (ListDiffMode to Synchronous)
    val asyncHints = containerHints + (ListDiffMode to Asynchronous)
    listStub.update(rendering.listRendering, containerHints)
    syncListStub.update(rendering.listRendering, syncHints)
    asyncListStub.update(rendering.listRendering, asyncHints)
    addNewRowButton.setOnClickListener { rendering.onAddRowTapped() }
  }
