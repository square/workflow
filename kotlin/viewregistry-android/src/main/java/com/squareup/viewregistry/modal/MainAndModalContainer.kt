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
package com.squareup.viewregistry.modal

import android.app.Dialog
import android.content.Context
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.Coordinators
import com.squareup.viewregistry.AlertScreen
import com.squareup.viewregistry.AlertScreen.Button.NEGATIVE
import com.squareup.viewregistry.AlertScreen.Button.NEUTRAL
import com.squareup.viewregistry.AlertScreen.Button.POSITIVE
import com.squareup.viewregistry.AlertScreen.Event.ButtonClicked
import com.squareup.viewregistry.AlertScreen.Event.Canceled
import com.squareup.viewregistry.BuilderBinding
import com.squareup.viewregistry.HandlesBack
import com.squareup.viewregistry.MainAndModalScreen
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.buildView
import io.reactivex.Observable
import io.reactivex.Observable.never
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.reflect.jvm.jvmName

/**
 * A container view that can display a stream of [MainAndModalScreen] instances.
 * Also provides special handling for [AlertScreen]s found in [MainAndModalScreen.modals],
 * using [AlertDialog] to show them.
 *
 * This view is back button friendly -- it implements [HandlesBack], delegating
 * to displayed views that implement that interface themselves.
 */
class MainAndModalContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {
  private val body: View? get() = getChildAt(0)
  private var dialogs: List<DialogRef> = emptyList()
  private val takeScreensSubs = CompositeDisposable()

  private val attached = BehaviorSubject.create<Boolean>()

  // Kind of a hack: use a Coordinator to create observable attached property.
  // TODO should probably just drop Coordinators for RxBinding.
  init {
    Coordinators.bind(this) {
      object : Coordinator() {
        override fun attach(view: View) {
          attached.onNext(true)
        }

        override fun detach(view: View) {
          attached.onNext(false)

          // TODO(https://github.com/square/workflow/issues/51)
          // Not good enough, the stupid Activity cleans it up and shames us about "leaks" in logcat
          // before this point. Try to use a lifecycle observer to clean that up.
          dialogs.forEach { it.dialog.hide() }
          dialogs = emptyList()
        }
      }
    }
  }

  override fun onBackPressed(): Boolean {
    // This should only be hit if there are no dialogs showing, so we only
    // need to consider the body.
    return (body as? HandlesBack)?.onBackPressed() == true
  }

  fun takeScreens(
    screens: Observable<out MainAndModalScreen<*, *>>,
    viewRegistry: ViewRegistry
  ) {
    takeScreensSubs.clear()

    // This looks like we're leaking subscriptions to screens, since we only unsubscribe
    // if takeScreens() is called again. It's fine, though:  the switchMap in whileAttached()
    // ensures that we're only subscribed to it while this view is attached to a window.

    // Create a new body view each time the type of [MainAndModalScreen.main] changes.
    takeScreensSubs.add(screens
        .whileAttached()
        .distinctUntilChanged { screen -> screen.main::class }
        .subscribe {
          removeAllViews()
          addView(it.viewForMain(screens, viewRegistry, this))
        }
    )

    // Compare the new modals list to the set of dialogs already on display. Show
    // any new ones, throw away any stale ones.
    takeScreensSubs.add(screens
        .whileAttached()
        .subscribe { mainAndModal ->
          val newDialogs = mutableListOf<DialogRef>()
          for ((i, modalScreen) in mainAndModal.modals.withIndex()) {
            newDialogs +=
              if (dialogs.size < i && dialogs[i].screen::class == modalScreen::class) {
                dialogs[i]
              } else {
                DialogRef(modalScreen, mainAndModal.showDialog(i, screens, viewRegistry, this))
              }
          }

          (dialogs - newDialogs).forEach { it.dialog.hide() }
          dialogs = newDialogs
        }
    )
  }

  private fun showAlert(alertScreen: AlertScreen): AlertDialog {
    val builder = AlertDialog.Builder(context)

    if (alertScreen.cancelable) {
      builder.setOnCancelListener { alertScreen.onEvent(Canceled) }
    } else {
      builder.setCancelable(false)
    }

    for ((button, name) in alertScreen.buttons) {
      when (button) {
        POSITIVE -> builder.setPositiveButton(name) { _, _ ->
          alertScreen.onEvent(ButtonClicked(POSITIVE))
        }
        NEGATIVE -> builder.setNegativeButton(name) { _, _ ->
          alertScreen.onEvent(ButtonClicked(NEGATIVE))
        }
        NEUTRAL -> builder.setNeutralButton(name) { _, _ ->
          alertScreen.onEvent(ButtonClicked(NEUTRAL))
        }
      }
    }

    alertScreen.message.takeIf { it.isNotBlank() }
        .let { builder.setMessage(it) }
    alertScreen.title.takeIf { it.isNotBlank() }
        .let { builder.setTitle(it) }

    return builder.show()
  }

  private fun <T> Observable<T>.whileAttached(): Observable<T> =
    attached.switchMap { isAttached -> if (isAttached) this else never<T>() }

  private class DialogRef(
    val screen: Any,
    val dialog: Dialog
  )

  private fun <D : Any> MainAndModalScreen<*, D>.showDialog(
    index: Int,
    mainAndModalScreens: Observable<out MainAndModalScreen<*, *>>,
    viewRegistry: ViewRegistry,
    container: ViewGroup
  ): Dialog {
    (modals[index] as? AlertScreen)?.let { return showAlert(it) }

    // https://github.com/square/workflow/issues/58
    TODO("Non-alerts are not supported yet.")
  }

  companion object : ViewBinding<MainAndModalScreen<*, *>>
  by BuilderBinding(
      type = MainAndModalScreen::class.java,
      builder = { screens, builders, context, _ ->
        MainAndModalContainer(context).apply {
          layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
          takeScreens(screens, builders)
        }
      }
  )
}

fun <T : Any> MainAndModalScreen<T, *>.viewForMain(
  mainAndModalScreens: Observable<out MainAndModalScreen<*, *>>,
  viewRegistry: ViewRegistry,
  container: ViewGroup
): View {
  val mainScreens: Observable<out T> = mainAndModalScreens.mapToMainMatching(this)
  val binding: ViewBinding<T> = viewRegistry.getBinding(main::class.jvmName)
  return binding.buildView(mainScreens, viewRegistry, container)
}

fun <T : Any> Observable<out MainAndModalScreen<*, *>>.mapToMainMatching(
  screen: MainAndModalScreen<T, *>
): Observable<out T> {
  return map { it.main }.ofType(screen.main::class.java)
}
