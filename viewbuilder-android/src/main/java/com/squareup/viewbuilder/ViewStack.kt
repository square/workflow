/*
 * Copyright 2017 Square Inc.
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
package com.squareup.viewbuilder

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.SparseArray
import android.view.View
import android.view.View.BaseSavedState
import com.squareup.viewbuilder.ViewStack.ReplaceResult.POPPED
import com.squareup.viewbuilder.ViewStack.ReplaceResult.PUSHED

/**
 * Maintains a stack of persisted view hierarchy states. When advised of a transition between
 * two views, [saves][View.saveHierarchyState] and [restores][View.restoreHierarchyState] their
 * states as needed, in the process reporting if the change was effectively a [push][PUSHED]
 * or a [pop][POPPED].
 */
class ViewStack private constructor(
  private var viewStates: List<ViewStateFrame>
) : Parcelable {
  constructor() : this(emptyList())

  /**
   * To be called when replacing [currentView] with [newView], before [newView]
   * is added to a parent. Saves and restores view states as necessary, and reports
   * whether we have seen [newView] before.
   *
   * If [newView] is one we've seen before, its state is restored and [POPPED] is returned.
   * Any view states that had been recorded after we last saw [newView] are dropped,
   * and [currentView]'s state is not saved.
   *
   * If [newView] is new to us, [currentView]'s state is saved and [PUSHED] is
   * returned.
   */
  fun replace(
    currentView: View,
    newView: View
  ): ReplaceResult {

    val popIndex = viewStates.indexOfFirst { it.key == newView.betterKey.toString() }
    if (popIndex < 0) {
      val saved = SparseArray<Parcelable>().apply { currentView.saveHierarchyState(this) }
      viewStates += ViewStateFrame(currentView.betterKey.toString(), saved)
      return PUSHED
    }

    viewStates = viewStates.subList(0, popIndex + 1)
    viewStates.lastOrNull()
        ?.let {
          newView.restoreHierarchyState(it.viewState)
          viewStates = viewStates.subList(0, viewStates.size - 1)
        }

    return POPPED
  }

  /** Return values for [replace], see details there. */
  enum class ReplaceResult {
    PUSHED,
    POPPED
  }

  /**
   * Convenience for use in [View.onSaveInstanceState] and [View.onRestoreInstanceState]
   * methods of container views that have no other state to save.
   *
   * More interesting containers should create their own [BaseSavedState]
   * subclass rather than trying to extend this one.
   */
  class SavedState : BaseSavedState {
    constructor(
      saving: Parcelable,
      viewStack: ViewStack
    ) : super(saving) {
      this.viewStack = viewStack
    }

    constructor(source: Parcel) : super(source) {
      this.viewStack = source.readParcelable(SavedState::class.java.classLoader)
    }

    val viewStack: ViewStack

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
      out.writeParcelable(viewStack, flags)
    }

    companion object CREATOR : Creator<SavedState> {
      override fun createFromParcel(source: Parcel) =
        SavedState(source)

      override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
    }
  }

  // region Parcelable

  override fun describeContents(): Int = 0

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {
    parcel.writeList(viewStates)
  }

  companion object CREATOR : Creator<ViewStack> {
    @Suppress("UNCHECKED_CAST")
    override fun createFromParcel(parcel: Parcel): ViewStack {
      val viewStates = parcel.readArrayList(ViewStack::class.java.classLoader)
          as ArrayList<ViewStateFrame>
      return ViewStack(viewStates)
    }

    override fun newArray(size: Int): Array<ViewStack?> = arrayOfNulls(size)
  }

  // endregion
}
