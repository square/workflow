package com.squareup.viewbuilder

data class MainAndModalScreen<out M : Any, out D : Any>(
  val main: M,
  val modals: List<D> = emptyList()
) {
  constructor(
    main: M,
    modal: D
  ) : this(main, listOf(modal))
}
