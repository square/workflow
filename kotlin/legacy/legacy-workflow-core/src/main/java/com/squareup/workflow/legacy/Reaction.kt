/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy

/**
 * Indicates whether a [Reactor] should enter another state ([EnterState]), with
 * [EnterState.state] as the next state, or [FinishWith] the result [FinishWith.result].
 */
@Suppress("UNUSED")
@Deprecated("Use com.squareup.workflow.Workflow")
sealed class Reaction<out S, out O>

/** Emit [state] as the next state to be passed to [Reactor.onReact]. */
@Deprecated("Use com.squareup.workflow.Workflow")
data class EnterState<out S>(val state: S) : Reaction<S, Nothing>()

/**
 * Stop reacting. After this is returned from [Reactor.onReact], it won't be called anymore.
 * If the [Reactor] is hosted by a [ReactorDriver], causes [ReactorDriver.result] to emit [result].
 */
@Deprecated("Use com.squareup.workflow.Workflow")
data class FinishWith<out O>(val result: O) : Reaction<Nothing, O>()
