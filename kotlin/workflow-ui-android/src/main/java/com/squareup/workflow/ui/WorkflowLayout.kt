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
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.Coordinators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A view that can be driven by a [WorkflowRunner]. In most cases you'll use
 * [Activity.setContentWorkflow][android.support.v4.app.FragmentActivity.setContentWorkflow]
 * or subclass [WorkflowFragment] rather than manage this class directly.
 */
@ExperimentalWorkflowUi
@UseExperimental(ExperimentalCoroutinesApi::class)
class WorkflowLayout(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {
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
    renderings: Flow<Any>,
    registry: ViewRegistry
  ) {
    takeWhileAttached(renderings) { show(it, registry) }
  }

  /**
   * Convenience override to start this layout from [renderings][WorkflowRunner.renderings]
   * and [viewRegistry][WorkflowRunner.viewRegistry] of [workflowRunner].
   */
  fun start(workflowRunner: WorkflowRunner<*>) {
    val renderings = workflowRunner.renderings
    val registry = workflowRunner.viewRegistry
    start(renderings, registry)
  }

  override fun onBackPressed(): Boolean {
    return showing
        ?.let { HandlesBack.Helper.onBackPressed(it) }
        ?: false
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
      @Suppress("UNCHECKED_CAST")
      this.childState = source.readSparseArray(SavedState::class.java.classLoader)
          as SparseArray<Parcelable>
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
    // TODO Inject this or something.
    val scope = CoroutineScope(Dispatchers.Main.immediate)
    Coordinators.bind(this) {
      object : Coordinator() {
        var job: Job? = null

        override fun attach(view: View) {
          job = scope.launch {
            source.collect { screen ->
              update(screen)
            }
          }
        }

        override fun detach(view: View) {
          job?.let {
            it.cancel()
            job = null
          }
        }
      }
    }
  }
}
