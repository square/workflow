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
package com.squareup.viewregistry

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.support.annotation.IdRes
import android.support.annotation.StyleRes
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.squareup.viewregistry.ModalContainer.Companion.forAlertContainerScreen
import com.squareup.viewregistry.ModalContainer.Companion.forContainerScreen
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.reflect.jvm.jvmName

/**
 * Base class for containers that show [HasModals.modals] in [Dialog]s.
 *
 * The concrete implementations returned by the factory methods [forAlertContainerScreen]
 * and [forContainerScreen] should cover many specific needs, and where those are too
 * limiting subclasses are simple to create.
 */
abstract class ModalContainer<M : Any>
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {

  private val base: View? get() = getChildAt(0)
  private var dialogs: List<DialogRef<M>> = emptyList()
  private val takeScreensSubs = CompositeDisposable()

  private var restoredBaseViewState: SparseArray<Parcelable>? = null
  private var restoredDialogBundles: List<TypeAndBundle>? = null

  private val attached = BehaviorSubject.createDefault<Boolean>(false)

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    attached.onNext(true)
  }

  override fun onDetachedFromWindow() {
    attached.onNext(false)

    // TODO(https://github.com/square/workflow/issues/51)
    // Not good enough, the stupid Activity cleans it up and shames us about "leaks" in logcat
    // before this point. Try to use a lifecycle observer to clean that up.
    dialogs.forEach { it.dialog.hide() }
    dialogs = emptyList()
    super.onDetachedFromWindow()
  }

  final override fun onBackPressed(): Boolean {
    // This should only be hit if there are no dialogs showing, so we only
    // need to consider the body.
    return base?.let { HandlesBack.Helper.onBackPressed(it) } == true
  }

  fun takeScreens(
    screens: Observable<out HasModals<*, M>>,
    viewRegistry: ViewRegistry
  ) {
    takeScreensSubs.clear()

    // This looks like we're leaking subscriptions to screens, since we only unsubscribe
    // if takeScreens() is called again. It's fine, though:  the switchMap in whileAttached()
    // ensures that we're only subscribed to it while this view is attached to a window.

    // Create a new body view each time the type of [AlertContainerScreen.base] changes.
    takeScreensSubs.add(screens
        .whileAttached()
        .distinctUntilChanged { containerScreen -> containerScreen.baseScreen::class }
        .subscribe {
          removeAllViews()
          val newView = it.viewForBase(screens, viewRegistry, this)
          restoredBaseViewState?.let {
            restoredBaseViewState = null
            newView.restoreHierarchyState(it)
          }
          addView(newView)
        }
    )

    // Compare the new modals list to the set of dialogs already on display. Show
    // any new ones, throw away any stale ones.
    takeScreensSubs.add(screens
        .whileAttached()
        .subscribe { containerScreen ->
          val newDialogs = mutableListOf<DialogRef<M>>()
          for ((i, modal) in containerScreen.modals.withIndex()) {
            newDialogs += if (i < dialogs.size && dialogs[i].screen.matches(modal)) {
              dialogs[i]
            } else {
              val modalsInThisLayer = screens.whileAttached()
                  .filter { i < it.modals.size }
                  .map { it.modals[i] }
              DialogRef(modal, containerScreen.showDialog(i, modalsInThisLayer, viewRegistry))
            }
          }

          (dialogs - newDialogs).forEach { it.dialog.hide() }

          restoredDialogBundles?.let {
            restoredDialogBundles = null
            if (it.size == newDialogs.size) {
              it.zip(newDialogs) { viewState, dialogRef -> dialogRef.restore(viewState) }
            }
          }
          dialogs = newDialogs
        }
    )
  }

  /**
   * Returns true if the new screen can be shown by the dialog that was created for the receiver.
   * Default implementation compares equality of the receiver and the new screen.
   */
  protected open fun M.matches(nextModal: M): Boolean = this == nextModal

  /**
   * Called to create and show a Dialog to render [modalScreen].
   */
  protected abstract fun showDialog(
    modalScreen: M,
    screens: Observable<out M>,
    viewRegistry: ViewRegistry
  ): Dialog

  override fun onSaveInstanceState(): Parcelable {
    return SavedState(
        super.onSaveInstanceState(),
        SparseArray<Parcelable>().also { array -> base?.saveHierarchyState(array) },
        dialogs.map { it.save() }
    )
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? SavedState)
        ?.let {
          restoredBaseViewState = it.baseViewState
          restoredDialogBundles = it.dialogBundles
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

  private fun HasModals<*, M>.showDialog(
    index: Int,
    screens: Observable<out M>,
    viewRegistry: ViewRegistry
  ): Dialog = showDialog(modals[index], screens, viewRegistry)

  private fun <T> Observable<T>.whileAttached(): Observable<T> =
    attached.switchMap { isAttached -> if (isAttached) this else Observable.never<T>() }

  internal data class TypeAndBundle(
    val screenType: String,
    val bundle: Bundle
  ) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(
      parcel: Parcel,
      flags: Int
    ) {
      parcel.writeString(screenType)
      parcel.writeBundle(bundle)
    }

    companion object CREATOR : Creator<TypeAndBundle> {
      override fun createFromParcel(parcel: Parcel): TypeAndBundle {
        val type = parcel.readString()
        val bundle = parcel.readBundle(TypeAndBundle::class.java.classLoader)
        return TypeAndBundle(type, bundle)
      }

      override fun newArray(size: Int): Array<TypeAndBundle?> = arrayOfNulls(size)
    }
  }

  private class DialogRef<M : Any>(
    val screen: M,
    val dialog: Dialog
  ) {
    fun save(): TypeAndBundle {
      val saved = dialog.window.saveHierarchyState()
      return TypeAndBundle(screen::class.jvmName, saved)
    }

    fun restore(typeAndBundle: TypeAndBundle) {
      if (screen::class.jvmName == typeAndBundle.screenType) {
        dialog.window.restoreHierarchyState(typeAndBundle.bundle)
      }
    }
  }

  private fun <T : Any> HasModals<T, *>.viewForBase(
    containerScreens: Observable<out HasModals<*, *>>,
    viewRegistry: ViewRegistry,
    container: ViewGroup
  ): View {
    val baseScreens: Observable<out T> = containerScreens.mapToBaseMatching(this)
    val binding: ViewBinding<T> = viewRegistry.getBinding(baseScreen::class.jvmName)
    return binding.buildView(baseScreens, viewRegistry, container)
  }

  private fun <T : Any> Observable<out HasModals<*, *>>.mapToBaseMatching(
    screen: HasModals<T, *>
  ): Observable<out T> = map { it.baseScreen }.ofType(screen.baseScreen::class.java)

  private fun <M : Any> Observable<out HasModals<*, *>>.mapToModalMatching(
    screen: HasModals<*, M>,
    index: Int
  ): Observable<out M> = filter { index < it.modals.size }
      .map { it.modals[index] }
      .ofType(screen.modals[index]::class.java)

  private class SavedState : BaseSavedState {
    constructor(
      superState: Parcelable,
      bodyState: SparseArray<Parcelable>,
      dialogBundles: List<TypeAndBundle>
    ) : super(superState) {
      this.baseViewState = bodyState
      this.dialogBundles = dialogBundles
    }

    constructor(source: Parcel) : super(source) {
      @Suppress("UNCHECKED_CAST")
      this.baseViewState = source.readSparseArray(SavedState::class.java.classLoader)
          as SparseArray<Parcelable>
      this.dialogBundles = mutableListOf<TypeAndBundle>().apply {
        source.readTypedList(this, TypeAndBundle.CREATOR)
      }
    }

    val baseViewState: SparseArray<Parcelable>
    val dialogBundles: List<TypeAndBundle>

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
      out.writeTypedList(dialogBundles)
    }

    companion object CREATOR : Creator<SavedState> {
      override fun createFromParcel(source: Parcel): SavedState = SavedState(source)

      override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
    }
  }

  companion object {
    /**
     * Creates a [ViewBinding] to show the [AlertScreen]s of an [AlertContainerScreen]
     * as Android `AlertDialog`s.
     *
     * @param dialogThemeResId the resource ID of the theme against which to inflate
     * dialogs. Defaults to `0` to use the parent `context`'s default alert dialog theme.
     */
    fun forAlertContainerScreen(
      @StyleRes dialogThemeResId: Int = 0
    ): ViewBinding<AlertContainerScreen<*>> = AlertContainer.Binding(dialogThemeResId)

    /**
     * Creates a [ViewBinding] for modal container screens of type [H].
     *
     * Each view created for [HasModals.modals] will be shown in a [Dialog]
     * whose window is set to size itself to `WRAP_CONTENT` (see [android.view.Window.setLayout]).
     * Two customization hooks are provided: you can specify a [theme][dialogThemeResId] to be
     * applied to the dialog window; and/or provide a [function][modalDecorator] to decorate
     * the view to set as the [dialog's content][Dialog.setContentView].
     *
     * @param id a unique identifier for containers of this type, allowing them to participate
     * view persistence
     *
     * @param dialogThemeResId a style resource describing the theme to use for dialog
     * windows. Defaults to `0` to use the default dialog theme.
     *
     * @param modalDecorator a function to apply to each [modal][M] view created before
     * it is passed to [android.app.Dialog.setContentView]. Defaults to making no changes.
     */
    inline fun <reified H : HasModals<*, *>> forContainerScreen(
      @IdRes id: Int,
      @StyleRes dialogThemeResId: Int = 0,
      noinline modalDecorator: (View) -> View = { it }
    ): ViewBinding<H> =
      ModalViewContainer.Binding(id, H::class.java, dialogThemeResId, modalDecorator)
  }
}
