/*
 * Copyright 2018 Square Inc.
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
package com.squareup.workflow.ui.backstack

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.SparseArray
import android.view.View
import android.view.View.BaseSavedState
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.ui.BackStackScreen.Key
import com.squareup.workflow.ui.R
import com.squareup.workflow.ui.backstack.ViewStateStack.Direction.POP
import com.squareup.workflow.ui.backstack.ViewStateStack.Direction.PUSH
import com.squareup.workflow.ui.backstack.ViewStateStack.SavedState
import com.squareup.workflow.ui.backstack.ViewStateStack.UpdateTools
import io.reactivex.Observable
import kotlin.reflect.full.cast

/**
 * Maintains a stack of persisted [view hierarchy states][View.saveHierarchyState].
 *
 * When preparing to show a new screen, call [prepareToUpdate] and use the [UpdateTools]
 * it returns.
 *
 * This class implements [Parcelable] so that it can be preserved from
 * a container view's own [View.saveHierarchyState] method -- call [save] first.
 * A simple container can return [SavedState] from that method rather than
 * creating its own persistence class.
 */
class ViewStateStack private constructor(
  private var viewStates: List<ViewStateFrame>
) : Parcelable {
  constructor() : this(emptyList())

  /**
   * Provides support to save the state of the current view and possibly restore
   * that of a new view. Returned by [prepareToUpdate].
   */
  interface UpdateTools {
    /** Describes whether this change will be a [PUSH] or a [POP]. */
    val direction: Direction

    /***
     * Method to run against the outgoing view to save its
     * [view hierarchy state][View.saveHierarchyState].
     */
    fun saveOldView(oldView: View) = Unit

    /**
     * Method to run against the incoming view to restore its previously saved
     * [view hierarchy state][View.saveHierarchyState], if there is any.
     * This should be called after the view is inflated / instantiated, and before
     * it is attached to a window.
     */
    fun setUpNewView(newView: View)
  }

  /** @see UpdateTools.direction */
  enum class Direction {
    PUSH,
    POP
  }

  /**
   * To be called when the container is ready to create and show the view for
   * a new [BackStackScreen]. Returns [UpdateTools] to help get the job done.
   */
  fun prepareToUpdate(newScreenKey: Key<*>): UpdateTools {
    val popIndex = viewStates.indexOfFirst { it.key == newScreenKey.toString() }
    if (popIndex < 0) {
      return object : UpdateTools {
        override val direction = PUSH

        override fun saveOldView(oldView: View) {
          val saved = SparseArray<Parcelable>().apply { oldView.saveHierarchyState(this) }
          viewStates += ViewStateFrame(
              oldView.backStackKey.toString(), saved
          )
        }

        override fun setUpNewView(newView: View) {
          newView.backStackKey = newScreenKey
        }
      }
    }

    return object : UpdateTools {
      override val direction = POP

      override fun setUpNewView(newView: View) {
        newView.backStackKey = newScreenKey
        viewStates = viewStates.subList(0, popIndex + 1)
        viewStates.lastOrNull()
            ?.let {
              newView.restoreHierarchyState(it.viewState)
              viewStates = viewStates.subList(0, viewStates.size - 1)
            }
      }
    }
  }

  /**
   * To be called from [View.saveHierarchyState] before serializing this instance,
   * to ensure that the state of the currently visible view is saved.
   */
  fun save(currentView: View) {
    val saved = SparseArray<Parcelable>().apply { currentView.saveHierarchyState(this) }
    val newFrame =
      ViewStateFrame(currentView.backStackKey.toString(), saved)
    if (!viewStates.isEmpty() && viewStates.last().key == currentView.backStackKey.toString()) {
      viewStates = viewStates.subList(0, viewStates.size - 1)
    }
    viewStates += newFrame
  }

  /**
   * Convenience for use in [View.onSaveInstanceState] and [View.onRestoreInstanceState]
   * methods of container views that have no other state or their own to save.
   *
   * More interesting containers should create their own subclass of [BaseSavedState]
   * rather than trying to extend this one.
   */
  class SavedState : BaseSavedState {
    constructor(
      saving: Parcelable,
      viewStateStack: ViewStateStack
    ) : super(saving) {
      this.viewStateStack = viewStateStack
    }

    constructor(source: Parcel) : super(source) {
      this.viewStateStack = source.readParcelable(SavedState::class.java.classLoader)
    }

    val viewStateStack: ViewStateStack

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
      out.writeParcelable(viewStateStack, flags)
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

  companion object CREATOR : Creator<ViewStateStack> {
    @Suppress("UNCHECKED_CAST")
    override fun createFromParcel(parcel: Parcel): ViewStateStack {
      val viewStates = parcel.readArrayList(ViewStateStack::class.java.classLoader)
          as ArrayList<ViewStateFrame>
      return ViewStateStack(viewStates)
    }

    override fun newArray(size: Int): Array<ViewStateStack?> = arrayOfNulls(size)
  }

  // endregion
}

/**
 * Ties a view to the [BackStackScreen.Key<*>] that created it.
 * Always set by [ViewStateStack.UpdateTools.setUpNewView].
 */
var View.backStackKey: Key<*>
  get() {
    return getTag(R.id.workflow_back_stack_key) as Key<*>?
        ?: throw IllegalArgumentException("No key found on $this")
  }
  private set(screenKey) = setTag(R.id.workflow_back_stack_key, screenKey)

/**
 * Maps an untyped stream of [BackStackScreen] down to the [BackStackScreen.wrapped]
 * that match the parameter type of [screen].
 */
fun <T : Any> Observable<out BackStackScreen<*>>.mapToWrappedMatching(
  screen: BackStackScreen<T>
): Observable<out T> {
  return filter { it.key == screen.key }.map {
    screen.key.type.cast(it.wrapped)
  }
}
