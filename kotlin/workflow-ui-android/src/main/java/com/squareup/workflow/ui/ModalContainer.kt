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
import android.widget.FrameLayout
import com.squareup.workflow.ui.HandlesBack.Helper
import com.squareup.workflow.ui.ModalContainer.Companion.forAlertContainerScreen
import com.squareup.workflow.ui.ModalContainer.Companion.forContainerScreen
import io.reactivex.Observable
import kotlin.reflect.jvm.jvmName

/**
 * Base class for containers that show [HasModals.modals] in [Dialog]s.
 *
 * The concrete implementations returned by the factory methods [forAlertContainerScreen]
 * and [forContainerScreen] should cover many specific needs, and where those are too
 * limiting subclasses are simple to create.
 *
 * @param ModalRenderingT the type of the nested renderings to be shown in a dialog window.
 */
@ExperimentalWorkflowUi
abstract class ModalContainer<ModalRenderingT : Any>
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {

  private val base: View? get() = getChildAt(0)

  private var dialogs: List<DialogRef<ModalRenderingT>> = emptyList()

  protected lateinit var registry: ViewRegistry

  final override fun onBackPressed(): Boolean {
    // This should only be hit if there are no dialogs showing, so we only
    // need to consider the body.
    return base?.let { Helper.onBackPressed(it) } == true
  }

  protected fun update(newScreen: HasModals<*, ModalRenderingT>) {
    base?.takeIf { it.canShowRendering(newScreen.baseScreen) }
        ?.showRendering(newScreen.baseScreen)
        ?: run {
          removeAllViews()
          val newBase = registry.buildView(newScreen.baseScreen, this)
          addView(newBase)
        }

    val newDialogs = mutableListOf<DialogRef<ModalRenderingT>>()
    for ((i, modal) in newScreen.modals.withIndex()) {
      newDialogs += if (i < dialogs.size && dialogs[i].modalRendering::class == modal::class) {
        dialogs[i].copy(modalRendering = modal)
            .also { updateDialog(it) }
      } else {
        buildDialog(modal, registry).apply { dialog.show() }
      }
    }

    (dialogs - newDialogs).forEach { it.dialog.hide() }
    dialogs = newDialogs
  }

  /**
   * Called to create (but not show) a Dialog to render [initialModalRendering].
   */
  protected abstract fun buildDialog(
    initialModalRendering: ModalRenderingT,
    viewRegistry: ViewRegistry
  ): DialogRef<ModalRenderingT>

  protected abstract fun updateDialog(dialogRef: DialogRef<ModalRenderingT>)

  override fun onSaveInstanceState(): Parcelable {
    return SavedState(
        super.onSaveInstanceState()!!,
        SparseArray<Parcelable>().also { array -> base?.saveHierarchyState(array) },
        dialogs.map { it.save() }
    )
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? SavedState)
        ?.let {
          if (it.dialogBundles.size == dialogs.size) {
            it.dialogBundles.zip(dialogs) { viewState, dialogRef -> dialogRef.restore(viewState) }
          }
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

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
        val type = parcel.readString()!!
        val bundle = parcel.readBundle(TypeAndBundle::class.java.classLoader)!!
        return TypeAndBundle(type, bundle)
      }

      override fun newArray(size: Int): Array<TypeAndBundle?> = arrayOfNulls(size)
    }
  }

  /**
   * @param extra optional hook to allow subclasses to associate extra data with this dialog,
   * e.g. its content view. Not considered for equality.
   */
  protected data class DialogRef<ModalRenderingT : Any>(
    val modalRendering: ModalRenderingT,
    val dialog: Dialog,
    val extra: Any? = null
  ) {
    internal fun save(): TypeAndBundle {
      val saved = dialog.window!!.saveHierarchyState()
      return TypeAndBundle(modalRendering::class.jvmName, saved)
    }

    internal fun restore(typeAndBundle: TypeAndBundle) {
      if (modalRendering::class.jvmName == typeAndBundle.screenType) {
        dialog.window!!.restoreHierarchyState(typeAndBundle.bundle)
      }
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as DialogRef<*>

      if (dialog != other.dialog) return false

      return true
    }

    override fun hashCode(): Int {
      return dialog.hashCode()
    }
  }

  private fun <BaseRenderingT : Any> Observable<out HasModals<*, *>>.mapToBaseMatching(
    screen: HasModals<BaseRenderingT, *>
  ): Observable<out BaseRenderingT> = map { it.baseScreen }.ofType(screen.baseScreen::class.java)

  private fun <ModalRenderingT : Any> Observable<out HasModals<*, *>>.mapToModalMatching(
    screen: HasModals<*, ModalRenderingT>,
    index: Int
  ): Observable<out ModalRenderingT> = filter { index < it.modals.size }
      .map { it.modals[index] }
      .ofType(screen.modals[index]::class.java)

  private class SavedState : BaseSavedState {
    constructor(
      superState: Parcelable?,
      bodyState: SparseArray<Parcelable>,
      dialogBundles: List<TypeAndBundle>
    ) : super(superState) {
      this.baseViewState = bodyState
      this.dialogBundles = dialogBundles
    }

    constructor(source: Parcel) : super(source) {
      @Suppress("UNCHECKED_CAST")
      this.baseViewState = source.readSparseArray(
          SavedState::class.java.classLoader
      )
          as SparseArray<Parcelable>
      this.dialogBundles = mutableListOf<TypeAndBundle>().apply {
        source.readTypedList(this, TypeAndBundle)
      }
    }

    val baseViewState: SparseArray<Parcelable>
    val dialogBundles: List<TypeAndBundle>

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
      @Suppress("UNCHECKED_CAST")
      out.writeSparseArray(baseViewState as SparseArray<Any>)
      out.writeTypedList(dialogBundles)
    }

    companion object CREATOR : Creator<SavedState> {
      override fun createFromParcel(source: Parcel): SavedState =
        SavedState(source)

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
     * @param modalDecorator a function to apply to each [modal][HasModals.modals] view
     * created before it is passed to [android.app.Dialog.setContentView].
     * Defaults to making no changes.
     */
    inline fun <reified H : HasModals<*, *>> forContainerScreen(
      @IdRes id: Int,
      @StyleRes dialogThemeResId: Int = 0,
      noinline modalDecorator: (View) -> View = { it }
    ): ViewBinding<H> = ModalViewContainer.Binding(
        id, H::class, dialogThemeResId, modalDecorator
    )
  }
}
