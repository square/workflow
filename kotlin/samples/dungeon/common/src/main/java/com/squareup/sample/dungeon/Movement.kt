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
package com.squareup.sample.dungeon

import java.util.EnumSet
import java.util.EnumSet.copyOf
import java.util.EnumSet.noneOf

/**
 * A simplified component-based direction vector in the game world.
 *
 * Wraps a mutable [EnumSet] with copying operations.
 *
 * @param directions The components of the direction of the vector.
 * @param cellsPerSecond The magnitude of the vector, in cells-per-second.
 */
data class Movement(
  private val directions: EnumSet<Direction> = noneOf(Direction::class.java),
  val cellsPerSecond: Float = 1f
) : Iterable<Direction> by directions {

  constructor(
    vararg directions: Direction,
    cellsPerSecond: Float = 1f
  ) : this(copyOf(directions.asList()), cellsPerSecond)

  operator fun contains(direction: Direction): Boolean = direction in directions
  operator fun plus(direction: Direction): Movement = with { it.add(direction) }
  operator fun minus(direction: Direction): Movement = with { it.remove(direction) }

  private inline fun with(block: (EnumSet<Direction>) -> Unit): Movement =
    copy(directions = directions.clone().also(block))
}
