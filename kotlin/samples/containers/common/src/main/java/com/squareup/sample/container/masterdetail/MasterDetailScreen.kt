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
package com.squareup.sample.container.masterdetail

/**
 * Rendering type for master / detail containers. Containers may choose to display
 * both children side by side, or repackage them in a [com.squareup.workflow.ui.BackStackScreen]
 * in a single pane.
 *
 * @param masterRendering typically a selection list
 *
 * @param detailRendering typically the details of an item selected in the [masterRendering],
 * or null if none has been selected. Common for this to be a
 * [com.squareup.workflow.ui.BackStackScreen].
 *
 * @param selectDefault optional function that requests a selection be made to fill a
 * null [detailRendering]. Split view containers may call this immediately to ensure
 * that a detail rendering is always visible.
 */
data class MasterDetailScreen(
  val masterRendering: Any,
  val detailRendering: Any? = null,
  val selectDefault: (() -> Unit)? = null
)
