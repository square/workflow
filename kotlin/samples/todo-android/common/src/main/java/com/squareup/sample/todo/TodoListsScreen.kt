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

/**
 * Rendering of the list of [TodoList]s.
 *
 * Note that these renderings are created by [TodoListsWorkflow], which is unaware of the
 * [selection], always leaving that field set to the default `-1` value.
 *
 * The entire concept of selection is owned by the parent [TodoListsAppWorkflow],
 * which may add that info to a copy of the child workflow's rendering.
 */
data class TodoListsScreen(
  val lists: List<TodoList>,
  val onRowClicked: (Int) -> Unit,
  val selection: Int = -1
)
