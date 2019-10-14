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
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.squareup.workflow.ui.ModalContainer.Companion.forAlertContainerScreen
import com.squareup.workflow.ui.ModalContainer.Companion.forContainerScreen

/**
 * Base class for containers that show [HasModals.modals] in [Dialog]s.
 *
 * The concrete implementations returned by the factory methods [forAlertContainerScreen]
 * and [forContainerScreen] should cover many specific needs, and where those are too
 * limiting subclasses are simple to create.
 *
 * @param ModalRenderingT the type of the nested renderings to be shown in a dialog window.
 */
abstract class ModalContainer<ModalRenderingT : Any> @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : FrameLayout(context, attributeSet, defStyle, defStyleRes) {

  private val baseView: WorkflowViewStub = WorkflowViewStub(context).also {
    addView(it, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
  }

  private var dialogs: List<DialogRef<ModalRenderingT>> = emptyList()

  protected lateinit var registry: ViewRegistry

  protected fun update(newScreen: HasModals<*, ModalRenderingT>) {
    baseView.update(newScreen.baseScreen, registry)

    val newDialogs = mutableListOf<DialogRef<ModalRenderingT>>()
    for ((i, modal) in newScreen.modals.withIndex()) {
      newDialogs += if (i < dialogs.size && compatible(dialogs[i].modalRendering, modal)) {
        dialogs[i].copy(modalRendering = modal)
            .also { updateDialog(it) }
      } else {
        buildDialog(modal, registry).apply {
          dialog.show()
          // Android makes a lot of logcat noise if it has to close the window for us. :/
          // https://github.com/square/workflow/issues/51
          dialog.lifecycleOrNull()
              ?.addObserver(OnDestroy { dialog.dismiss() })
        }
      }
    }

    (dialogs - newDialogs).forEach { it.dialog.dismiss() }
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

  internal data class KeyAndBundle(
    val compatibilityKey: String,
    val bundle: Bundle
  ) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(
      parcel: Parcel,
      flags: Int
    ) {
      parcel.writeString(compatibilityKey)
      parcel.writeBundle(bundle)
    }

    companion object CREATOR : Creator<KeyAndBundle> {
      override fun createFromParcel(parcel: Parcel): KeyAndBundle {
        val key = parcel.readString()!!
        val bundle = parcel.readBundle(KeyAndBundle::class.java.classLoader)!!
        return KeyAndBundle(key, bundle)
      }

      override fun newArray(size: Int): Array<KeyAndBundle?> = arrayOfNulls(size)
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
    internal fun save(): KeyAndBundle {
      val saved = dialog.window!!.saveHierarchyState()
      return KeyAndBundle(Named.keyFor(modalRendering), saved)
    }

    internal fun restore(keyAndBundle: KeyAndBundle) {
      if (Named.keyFor(modalRendering) == keyAndBundle.compatibilityKey) {
        dialog.window!!.restoreHierarchyState(keyAndBundle.bundle)
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

  private class SavedState : BaseSavedState {
    constructor(
      superState: Parcelable?,
      dialogBundles: List<KeyAndBundle>
    ) : super(superState) {
      this.dialogBundles = dialogBundles
    }

    constructor(source: Parcel) : super(source) {
      @Suppress("UNCHECKED_CAST")
      this.dialogBundles = mutableListOf<KeyAndBundle>().apply {
        source.readTypedList(this, KeyAndBundle)
      }
    }

    val dialogBundles: List<KeyAndBundle>

    override fun writeToParcel(
      out: Parcel,
      flags: Int
    ) {
      super.writeToParcel(out, flags)
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

private class OnDestroy(private val block: () -> Unit) : LifecycleObserver {
  @OnLifecycleEvent(ON_DESTROY)
  fun onDestroy() = block()
}

private fun Dialog.lifecycleOrNull(): Lifecycle? = decorView?.context?.lifecycleOrNull()

private val Dialog.decorView: View?
  get() = window?.decorView

/**
 * The [Lifecycle] for this context, or null if one can't be found.
 *
 * We keep all this very forgiving because we're just using it to keep some logging
 * noise out of logcat. If someone manages to run this under a strange context whose
 * [Lifecycle] we can't find, just return null and let the caller no-op.
 */
private tailrec fun Context.lifecycleOrNull(): Lifecycle? = when (this) {
  is LifecycleOwner -> this.lifecycle
  else -> (this as? ContextWrapper)?.baseContext?.lifecycleOrNull()
}
