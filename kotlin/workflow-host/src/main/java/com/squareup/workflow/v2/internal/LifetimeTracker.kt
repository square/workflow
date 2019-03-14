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
package com.squareup.workflow.v2.internal

/**
 * Tracks the lifetime of child state machines and subscriptions, by some [key][KeyT].
 *
 * Update the set of tracked objects by calling [track]. When a new key is detected, it is
 * [started][start]. When a key is no longer present, it is [cancelled][dispose].
 *
 * Not thread-safe.
 *
 * @param FactoryT "Factory" for disposables.
 * @param KeyT The "Key" type that identifies unique factories.
 * @param DisposableT The type of "Disposable" that identifies a single live "lifetime" spawned from a
 * factory, and should be disposed of when no longer tracked.
 *
 * @param getKey Derives the uniqueness key from a [FactoryT].
 * @param start Starts an [DisposableT] from a [FactoryT]. Called when this object starts tracking a
 * new [FactoryT].
 * @param dispose Called when a [FactoryT] stops being tracked and its [DisposableT] should be
 * disposed.
 */
internal class LifetimeTracker<FactoryT, KeyT : Any, DisposableT>(
  private val getKey: (FactoryT) -> KeyT,
  private val start: (FactoryT) -> DisposableT,
  private val dispose: (FactoryT, DisposableT) -> Unit
) {

  private val factoriesByKey = mutableMapOf<KeyT, FactoryT>()
  private val disposablesByKey = mutableMapOf<KeyT, DisposableT>()

  val lifetimes: Map<FactoryT, DisposableT>
    get() = factoriesByKey.entries.map { (key, provider) ->
      val lifetime = disposablesByKey.getValue(key)
      provider to lifetime
    }.toMap()

  /**
   * Additively track a possibly-new provider.
   */
  fun ensure(factory: FactoryT): DisposableT {
    val key = getKey(factory)
    return ensureStarted(key, factory)
  }

  /**
   * [Starts][start] [factories] with [keys][getKey] not already present in this manager,
   * and [disposes+removes][dispose] disposables for those that aren't present in [factories].
   */
  fun track(factories: Iterable<FactoryT>) {
    val latestKeys = factories.associateBy(getKey)

    for ((key, factory) in latestKeys) {
      ensureStarted(key, factory)
    }

    // Iterate over a copy to avoid concurrent modification.
    for ((key, factory) in factoriesByKey.toList()) {
      if (key !in latestKeys) {
        dispose(key, factory)
      }
    }
  }

  private fun ensureStarted(
    key: KeyT,
    factory: FactoryT
  ): DisposableT {
    return if (key !in factoriesByKey) {
      factoriesByKey[key] = factory
      start(factory).also {
        disposablesByKey[key] = it
      }
    } else {
      disposablesByKey.getValue(key)
    }
  }

  private fun dispose(
    key: KeyT,
    factory: FactoryT
  ) {
    factoriesByKey -= key
    val lifetime = disposablesByKey.remove(key)!!
    dispose(factory, lifetime)
  }
}
