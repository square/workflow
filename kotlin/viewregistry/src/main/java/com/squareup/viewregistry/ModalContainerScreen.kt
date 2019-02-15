package com.squareup.viewregistry

data class ModalContainerScreen<out B : Any, out M : Any>(
  override val baseScreen: B,
  override val modals: List<M> = emptyList()
) : HasModals<B, M> {
  constructor(
    baseScreen: B,
    modal: M
  ) : this(baseScreen, listOf(modal))

  constructor(
    baseScreen: B,
    vararg modals: M
  ) : this(baseScreen, modals.toList())
}
