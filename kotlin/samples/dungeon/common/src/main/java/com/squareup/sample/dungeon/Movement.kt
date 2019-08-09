package com.squareup.sample.dungeon

import java.util.EnumSet
import java.util.EnumSet.copyOf
import java.util.EnumSet.noneOf

/**
 * Represents movement in one or more [Direction]s.
 *
 * Wraps a mutable [EnumSet] with copying operations.
 */
data class Movement(
  private val directions: EnumSet<Direction> = noneOf(Direction::class.java)
) : Iterable<Direction> by directions {
  constructor(vararg directions: Direction) : this(copyOf(directions.asList()))

  operator fun contains(direction: Direction): Boolean = direction in directions
  operator fun plus(direction: Direction): Movement = with { it.add(direction) }
  operator fun minus(direction: Direction): Movement = with { it.remove(direction) }

  private inline fun with(block: (EnumSet<Direction>) -> Unit): Movement =
    Movement(directions.clone().also(block))
}
