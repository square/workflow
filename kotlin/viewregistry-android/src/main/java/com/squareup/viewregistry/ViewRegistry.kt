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
package com.squareup.viewregistry

/**
 * A collection of [ViewBinding]s and [BackStackEffect]s that can be used to render
 * the stream of screen models emitted by a workflow (via [ViewBinding]), and the
 * visual transitions between them (via [BackStackEffect]).
 *
 * Two concrete [ViewBinding] implementations are provided:
 *
 *  - [LayoutBinding], allowing the easy pairing of Android XML layout resources with
 *    [Coordinator][com.squareup.coordinators.Coordinator]s to drive them.
 *
 *  - [BuilderBinding], which can build views from code.
 *
 *  Registries can be assembled via concatenation, making it easy to snap together screen sets.
 *  For example:
 *
 *     val AuthViewBindings = ViewRegistry(
 *         AuthorizingCoordinator, LoginCoordinator, SecondFactorCoordinator
 *     )
 *
 *     val TicTacToeViewBindings = ViewRegistry(
 *         NewGameCoordinator, GamePlayCoordinator, GameOverCoordinator
 *     ) + NoEffect(from = Key(GamePlayScreen::class.java), to = Key(GameOverScreen::class.java))
 *
 *     val ApplicationViewBindings = ViewRegistry(ApplicationCoordinator) +
 *         AuthViewBindings + TicTacToeViewBindings + PushPopEffect
 *
 * In the above example, note that the `companion object`s of the various `Coordinator` classes
 * honor a convention of implementing [ViewBinding], in aid of this kind of assembly. See the
 * class doc on [LayoutBinding] for details.
 */
class ViewRegistry private constructor(
  private val bindings: Map<String, ViewBinding<*>>,
  private val effects: List<BackStackEffect>
) {
  constructor(vararg bindings: ViewBinding<*>) : this(
      bindings.map { it.type to it }.toMap().apply {
        check(keys.size == bindings.size) {
          "${bindings.map { it.type }} should not have duplicate entries."
        }
      },
      emptyList()
  )

  constructor(vararg registries: ViewRegistry) : this(
      registries.map { it.bindings }
          .reduce { left, right ->
            val duplicateKeys = left.keys.intersect(right.keys)
            check(duplicateKeys.isEmpty()) { "Should not have duplicate entries $duplicateKeys." }
            left + right
          },
      registries.map { it.effects }
          .reduce { left, right -> left + right }
  )

  // This is why I can't make the type field up there Class<T>. If I change this
  // method to get(type: Class<T>) the return type is coerced to ViewBuilder<out T>,
  // and everything falls apart.
  //
  // https://github.com/square/workflow/issues/18
  fun <T : Any> getBinding(type: String): ViewBinding<T> {
    require(type in bindings) { "Unrecognized screen type $type" }

    @Suppress("UNCHECKED_CAST")
    return bindings[type] as ViewBinding<T>
  }

  /**
   * Returns the first registered [BackStackEffect] that [matches][BackStackEffect.matches], or else
   * [NoEffect] if none is found. Effects are considered in the order they are added to the
   * registry.
   */
  fun getEffect(
    from: BackStackScreen.Key<*>,
    to: BackStackScreen.Key<*>,
    direction: ViewStateStack.Direction
  ): BackStackEffect {
    return effects.firstOrNull { it.matches(from, to, direction) } ?: NoEffect
  }

  operator fun <T : Any> plus(binding: ViewBinding<T>): ViewRegistry {
    check(binding.type !in bindings.keys) {
      "Already registered ${bindings[binding.type]} for ${binding.type}, cannot accept $binding."
    }

    return ViewRegistry(bindings + (binding.type to binding), effects)
  }

  operator fun plus(effect: BackStackEffect): ViewRegistry {
    return ViewRegistry(bindings, effects + effect)
  }

  operator fun plus(registry: ViewRegistry): ViewRegistry {
    return ViewRegistry(this, registry)
  }
}
