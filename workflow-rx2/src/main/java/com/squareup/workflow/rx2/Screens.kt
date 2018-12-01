package com.squareup.workflow.rx2

import com.squareup.workflow.AnyScreen
import com.squareup.workflow.Screen
import com.squareup.workflow.Screen.Key
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
