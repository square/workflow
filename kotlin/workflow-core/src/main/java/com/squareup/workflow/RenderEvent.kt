package com.squareup.workflow

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Placeholder to use in renderings that can emit events, e.g. to show a toast.
 */
class RenderEvent<T>(internal val events: ReceiveChannel<T>)

/**
 * TODO kdoc
 */
interface RenderScope {
  fun <T> RenderEvent<T>.handle(block: (T) -> Unit)
}
