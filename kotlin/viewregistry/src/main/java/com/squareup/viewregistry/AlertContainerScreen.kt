package com.squareup.viewregistry

/**
 * May show a stack of [AlertScreen] over a [baseScreen].
 *
 * @param B the type of [baseScreen]
 */
data class AlertContainerScreen<B : Any>(
  override val baseScreen: B,
  override val modals: List<AlertScreen> = emptyList()
) : HasModals<B, AlertScreen> {
  constructor(
    baseScreen: B,
    alert: AlertScreen
  ) : this(baseScreen, listOf(alert))

  constructor(
    baseScreen: B,
    vararg alerts: AlertScreen
  ) : this(baseScreen, alerts.toList())
}
