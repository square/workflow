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
package com.squareup.workflow.ui

import kotlin.reflect.jvm.jvmName

/**
 * Allows renderings that do not implement [Compatible] themselves to be distinguished
 * by more than just their type. Instances are [compatible] if they have the same name
 * and have [compatible] [wrapped] fields.
 */
data class Named<W : Any>(
  val wrapped: W,
  val name: String
) : Compatible<Named<W>> {
  init {
    require(name.isNotBlank()) { "name must not be blank." }
  }

  /**
   * Used as a comparison key by [isCompatibleWith]. Handy for use as a map key.
   */
  val key: String = ((wrapped as? Named<*>)?.key ?: wrapped::class.jvmName) + "-Named($name)"

  override fun isCompatibleWith(another: Named<W>): Boolean {
    return this.key == another.key && compatible(this.wrapped, another.wrapped)
  }

  override fun toString(): String {
    return "${super.toString()}: $key"
  }
}
