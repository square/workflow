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
package com.squareup.workflow.ui

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.widget.FrameLayout
import io.reactivex.Observable
import io.reactivex.disposables.SerialDisposable

/**
 * A view that can be driven by a [WorkflowRunner]. In most cases you'll use
 * [Activity.setContentWorkflow][setContentWorkflow] or subclass [WorkflowFragment]
 * rather than manage this class directly.
 */
class WorkflowLayout(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
  private var restoredChildState: SparseArray<Parcelable>? = null
  private val showing: View? get() = if (childCount > 0) getChildAt(0) else null

  /**
   * Subscribes to [renderings], and uses [registry] to
   * [build a new view][ViewRegistry.buildView] each time a new type of rendering is received,
   * making that view the only child of this one.
   *
   * Views created this way may make recursive calls to [ViewRegistry.buildView] to make
   * children of their own to handle nested renderings.
   */
  fun start(
    renderings: Observable<out Any>,
    registry: ViewRegistry
  ) {
    takeWhileAttached(renderings) { show(it, registry) }
  }

  private fun show(
    newRendering: Any,
    registry: ViewRegistry
  ) {
    showing?.takeIf { it.canShowRendering(newRendering) }
        ?.let { it ->
          it.showRendering(newRendering)
          return
        }

    removeAllViews()
    val newView = registry.buildView(newRendering, this)
    restoredChildState?.let { restoredState ->
      restoredChildState = null
      newView.restoreHierarchyState(restoredState)
    }
    addView(newView)
  }

  override fun onSaveInstanceState(): Parcelable? {
    return SavedState(
        super.onSaveInstanceState()!!,
        SparseArray<Parcelable>().also { array -> showing?.saveHierarchyState(array) }
    )
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    (state as? SavedState)
        ?.let {
          restoredChildState = it.childState
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

  private class SavedState : BaseSavedState {
    constructor(
      superState: Parcelable?,
      childState: SparseArray<Parcelable>
    ) : super(superState) {
      this.childState = childState
    }

    constructor(source: Parcel) : super(source) {
      this.childState = source.readSparseArray<Parcelable>(SavedState::class.java.classLoader)!!
    }

    val childState: SparseArray<Parcelable>

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
      @Suppress("UNCHECKED_CAST")
      out.writeSparseArray(childState as SparseArray<Any>)
    }

    companion object CREATOR : Creator<SavedState> {
      override fun createFromParcel(source: Parcel): SavedState =
        SavedState(source)

      override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
    }
  }

  /**
   * Subscribes [update] to [source] only while this [View] is attached to a window.
   */
  private fun <S : Any> View.takeWhileAttached(
    source: Observable<S>,
    update: (S) -> Unit
  ) {
    val listener = object : OnAttachStateChangeListener {
      var sub = SerialDisposable()

      override fun onViewAttachedToWindow(v: View?) {
        sub.replace(source.subscribe { screen -> update(screen) })
      }

      override fun onViewDetachedFromWindow(v: View?) {
        sub.dispose()
      }
    }

    this.addOnAttachStateChangeListener(listener)
  }
}
