/*
 * Copyright 2018 Square Inc.
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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.squareup.workflow.legacy.Workflow
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.rx2.openSubscription
import com.squareup.workflow.legacy.switchMapState as coreSwitchMapState

/**
 * Returns a [Completable] that fires either when [Workflow.result] fires, or when
 * [Workflow.cancel] is called.
 */
@Suppress("unused")
@Deprecated("Use com.squareup.workflow.Workflow")
fun Workflow<*, *, *>.toCompletable(): Completable = state.ignoreElements()

/**
 * Like [mapState][com.squareup.workflow.mapState], transforms the receiving workflow with
 * [Workflow.state] of type [S1] to one with states of [S2]. Unlike that method, each [S1] update is
 * transformed into a stream of [S2] updates -- useful when an [S1] state might wrap an underlying
 * workflow whose own screens need to be shown.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.switchMapState(
  transform: (S1) -> Observable<out S2>
): Workflow<S2, E, O> = coreSwitchMapState {
  transform(it).openSubscription()
}
