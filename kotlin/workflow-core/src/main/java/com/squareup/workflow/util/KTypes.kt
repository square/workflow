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
package com.squareup.workflow.util

import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.KVariance.INVARIANT

/**
 * Helpers for creating common [KType]s.
 */
object KTypes {

  /**
   * Creates a [KType] for a type like `Foo<Bar>` that knows about both `Foo` and `Bar`.
   * E.g. `KTypes.fromGenericType(Foo::class, Bar::class)`
   *
   * The type projection's variance is hardcoded to [INVARIANT] in the returned [KType].
   */
  fun fromGenericType(
    type: KClassifier,
    typeArgument: KClassifier
  ): KType =
  // TODO https://github.com/square/workflow/issues/211 When we can use real reflection, replace
  // this with: type.createType(listOf(typeArgument.project()))
    FakeKType(type, listOf(typeArgument.project()))
}

/**
 * Fake, simplified version of [kotlin.reflect.KType] since the version of Buck we're using
 * internally blows up whenever you try doing anything more than `::class` with reflection.
 * See [#211](https://github.com/square/workflow/issues/211).
 */
private data class FakeKType(
  override val classifier: KClassifier,
  override val arguments: List<KTypeProjection>,
  override val isMarkedNullable: Boolean = false
) : KType {
  override fun toString(): String = "$classifier<${arguments.joinToString()}>"
}

private fun KClassifier.project(variance: KVariance = INVARIANT): KTypeProjection =
  KTypeProjection(variance, fakeStarProjectedType)

/**
 * Workaround for https://github.com/square/workflow/issues/211, for
 * [kotlin.reflect.full.starProjectedType].
 *
 * TODO https://github.com/square/workflow/issues/211 When we can use real reflection, delete.
 */
private val KClassifier.fakeStarProjectedType: KType
  get() = FakeKType(this, listOf(KTypeProjection.STAR))
