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
import kotlin.reflect.jvm.jvmName

/**
 * The root layout for a UI driven by [WorkflowActivityRunner].
 * Installed via [android.support.v4.app.FragmentActivity.setContentWorkflow].
 * See [setWorkflowRunner] for details.
 */
@ExperimentalWorkflowUi
internal class WorkflowLayout(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {
  private var restoredChildState: SparseArray<Parcelable>? = null
  private val showing: View? get() = if (childCount > 0) getChildAt(0) else null

  /**
   * Subscribes to [WorkflowActivityRunner.renderings]. Uses [WorkflowActivityRunner.viewRegistry]
   * to [build a new view][ViewRegistry.getBinding] each time a new type of rendering is received,
   * making that view the only child of this one.
   *
   * Views created this way are expected to monitor the rendering stream themselves to stay
   * up to date, and may also make recursive calls to [ViewRegistry.getBinding] to make
   * children of their own to handle nested renderings.
   */
  fun setWorkflowRunner(workflowRunner: WorkflowActivityRunner<*, *>) {
    takeWhileAttached(
        workflowRunner.renderings.distinctUntilChanged { rendering -> rendering::class }) {
      show(it, workflowRunner.renderings.ofType(it::class.java), workflowRunner.viewRegistry)
    }
  }

  override fun onBackPressed(): Boolean {
    return showing
        ?.let { HandlesBack.Helper.onBackPressed(it) }
        ?: false
  }

  private fun show(
    newRendering: Any,
    renderings: Observable<out Any>,
    viewRegistry: ViewRegistry
  ) {
    removeAllViews()
    val binding = viewRegistry.getBinding<Any>(newRendering::class.jvmName)
    val newView = binding.buildView(renderings, viewRegistry, this)
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
}
