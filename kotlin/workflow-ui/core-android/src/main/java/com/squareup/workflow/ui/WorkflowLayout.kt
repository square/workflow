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
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A view that can be driven by a [WorkflowRunner]. In most cases you'll use
 * [Activity.setContentWorkflow][setContentWorkflow] or subclass [WorkflowFragment]
 * rather than manage this class directly.
 */
class WorkflowLayout(
  context: Context,
  attributeSet: AttributeSet? = null
) : ViewGroup(context, attributeSet) {
  private val showing: WorkflowViewStub = WorkflowViewStub(context).also {
    addView(it, LayoutParams(MATCH_PARENT, MATCH_PARENT))
  }

  private var restoredChildState: SparseArray<Parcelable>? = null

  /**
   * Subscribes to [renderings], and uses [registry] to
   * [build a new view][ViewRegistry.buildView] each time a new type of rendering is received,
   * making that view the only child of this one.
   */
  fun start(
    renderings: Flow<Any>,
    registry: ViewRegistry
  ) {
    start(renderings, ViewEnvironment(registry))
  }

  /**
   * Subscribes to [renderings], and uses the [ViewRegistry] in the given [environment] to
   * [build a new view][ViewRegistry.buildView] each time a new type of rendering is received,
   * making that view the only child of this one.
   */
  fun start(
    renderings: Flow<Any>,
    environment: ViewEnvironment
  ) {
    val hintsWithDefaults = environment.withDefaultViewFactories()
    takeWhileAttached(renderings) { show(it, hintsWithDefaults) }
  }

  private fun ViewEnvironment.withDefaultViewFactories(): ViewEnvironment =
    this + (ViewRegistry to (this[ViewRegistry] + defaultViewFactories))

  private fun show(
    newRendering: Any,
    environment: ViewEnvironment
  ) {
    showing.update(newRendering, environment)
    restoredChildState?.let { restoredState ->
      restoredChildState = null
      showing.actual.restoreHierarchyState(restoredState)
    }
  }

  override fun onSaveInstanceState(): Parcelable? {
    return SavedState(
        super.onSaveInstanceState()!!,
        SparseArray<Parcelable>().also { array -> showing.actual.saveHierarchyState(array) }
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

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    showing.actual.layout(0, 0, measuredWidth, measuredHeight)
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    showing.actual.measure(widthMeasureSpec, heightMeasureSpec)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  private class SavedState : BaseSavedState {
    constructor(
      superState: Parcelable?,
      childState: SparseArray<Parcelable>
    ) : super(superState) {
      this.childState = childState
    }

    constructor(source: Parcel) : super(source) {
      this.childState = source.readSparseArray(SavedState::class.java.classLoader)!!
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
    source: Flow<S>,
    update: (S) -> Unit
  ) {
    val listener = object : OnAttachStateChangeListener {
      val scope = CoroutineScope(Dispatchers.Main.immediate)
      var job: Job? = null

      @OptIn(ExperimentalCoroutinesApi::class)
      override fun onViewAttachedToWindow(v: View?) {
        job = source.onEach { screen -> update(screen) }
            .launchIn(scope)
      }

      override fun onViewDetachedFromWindow(v: View?) {
        job?.cancel()
        job = null
      }
    }

    this.addOnAttachStateChangeListener(listener)
  }
}
