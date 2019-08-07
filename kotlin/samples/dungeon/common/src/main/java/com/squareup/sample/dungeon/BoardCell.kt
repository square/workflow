package com.squareup.sample.dungeon

data class BoardCell(val codePoint: Int) {
  constructor(emoji: String) : this(emoji.codePointAt(0))

  init {
    require(Character.isValidCodePoint(codePoint))
  }

  private val string = String(Character.toChars(codePoint))

  override fun toString(): String = string
}
