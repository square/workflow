package com.squareup.viewregistry

data class PanelContainerScreen<B : Any>(
  override val baseScreen: B,
  override val modals: List<BackStackScreen<*>> = emptyList()
) : IsModalContainerScreen<B, BackStackScreen<*>> {
  constructor(
    baseScreen: B,
    panel: BackStackScreen<*>
  ) : this(baseScreen, listOf(panel))

  constructor(
    baseScreen: B,
    vararg panels: BackStackScreen<*>
  ) : this(baseScreen, panels.toList())

  constructor(
    baseScreen: B,
    panel: Any
  ) : this(baseScreen, listOf(BackStackScreen(panel)))

  constructor(
    baseScreen: B,
    vararg panels: Any
  ) : this(baseScreen, panels.map { BackStackScreen(it) })
}
