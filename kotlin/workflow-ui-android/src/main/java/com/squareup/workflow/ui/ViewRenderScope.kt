package com.squareup.workflow.ui

import android.view.View
import com.squareup.workflow.RenderEvent
import com.squareup.workflow.RenderScope
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * TODO write documentation
 */
internal class ViewRenderScope(private val view: View) : RenderScope {

  override fun <T> RenderEvent<T>.handle(block: (T) -> Unit) {
    view.launchWhileAttached {  }
  }

  internal fun clearHandlers() {
    TODO()
  }
}

private fun View.launchWhileAttached(
  context: CoroutineContext,
  block: suspend CoroutineScope.() -> Unit
) {
  TODO()
}
