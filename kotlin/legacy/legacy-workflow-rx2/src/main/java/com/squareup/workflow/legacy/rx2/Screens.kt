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
package com.squareup.workflow.legacy.rx2

import com.squareup.workflow.legacy.AnyScreen
import com.squareup.workflow.legacy.Screen
import com.squareup.workflow.legacy.Screen.Key
import io.reactivex.Observable

/**
 * **NB:** This class is not quite deprecated yet, but it will be _very_ soon.
 *
 * Transforms a stream of arbitrarily typed [Screen]s to a stream of screens
 * that match a given [Screen.Key] exactly (including both [Screen.Key.typeName] and
 * [Screen.Key.value]), cast appropriately. For each stack received, works from
 * the top (index zero) and emits the first matching screen, or emits nothing if there is no match.
 *
 *    myWorkflow.state
 *        .map(wfState -> wfState.state[SOME_LAYER]!!)
 *        .ofKeyType(someKey)
 */
fun <D : Any, E : Any> Observable<out AnyScreen>.ofKeyType(key: Key<D, E>):
    Observable<Screen<D, E>> {
  return filter { it.key == key }
      .map {
        // The types had better match if the keys match.
        @Suppress("UNCHECKED_CAST")
        it as Screen<D, E>
      }
}
