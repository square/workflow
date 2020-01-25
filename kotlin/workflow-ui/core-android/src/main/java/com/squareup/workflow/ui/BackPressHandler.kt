package com.squareup.workflow.ui

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner

/**
 * A function passed to [View.backPressedHandler], to be called if the back
 * button is pressed while that view is attached to a window.
 */
typealias BackPressHandler = () -> Unit

/**
 * A function to be called if the device back button is pressed while this
 * view is attached to a window.
 *
 * Implemented via a [OnBackPressedCallback], making this a
 * last-registered-first-served mechanism.
 */
var View.backPressedHandler: BackPressHandler?
  get() = handlerWrapperOrNull?.handler
  set(value) {
    handlerWrapperOrNull?.stop()

    val wrapper = value?.let {
      HandleBackPressWhenAttached(this, it).apply { start() }
    }
    setTag(R.id.view_back_handler, wrapper)
  }

private val View.handlerWrapperOrNull
  get() = getTag(R.id.view_back_handler) as HandleBackPressWhenAttached?

/**
 * Uses the [androidx.activity.OnBackPressedDispatcher] to associate a [BackPressHandler]
 * with a [View].
 *
 * Registers [handler] whenever [view] is attached to a window, and removes it
 * whenever [view] is detached.
 */
private class HandleBackPressWhenAttached(
  private val view: View,
  val handler: BackPressHandler
) : OnAttachStateChangeListener {
  private val onBackPressedCallback = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      handler.invoke()
    }
  }

  fun start() {
    view.addOnAttachStateChangeListener(this)
    if (view.isAttachedToWindow) onViewAttachedToWindow(view)
  }

  fun stop() {
    if (view.isAttachedToWindow) onViewDetachedFromWindow(view)
    view.removeOnAttachStateChangeListener(this)
  }

  override fun onViewDetachedFromWindow(detachedView: View) {
    require(view === detachedView)
    onBackPressedCallback.remove()
  }

  override fun onViewAttachedToWindow(attachedView: View) {
    require(view === attachedView)
    view.context.onBackPressedDispatcherOwnerOrNull()
        ?.let { owner ->
          owner.onBackPressedDispatcher.addCallback(owner, onBackPressedCallback)
        }
  }
}

tailrec fun Context.onBackPressedDispatcherOwnerOrNull(): OnBackPressedDispatcherOwner? =
  when (this) {
    is OnBackPressedDispatcherOwner -> this
    else -> (this as? ContextWrapper)?.baseContext?.onBackPressedDispatcherOwnerOrNull()
  }
