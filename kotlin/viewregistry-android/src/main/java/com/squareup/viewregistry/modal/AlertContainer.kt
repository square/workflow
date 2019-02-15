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

import android.content.Context
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.Coordinators
import com.squareup.viewregistry.Alert
import com.squareup.viewregistry.Alert.Button.NEGATIVE
import com.squareup.viewregistry.Alert.Button.NEUTRAL
import com.squareup.viewregistry.Alert.Button.POSITIVE
import com.squareup.viewregistry.Alert.Event.ButtonClicked
import com.squareup.viewregistry.Alert.Event.Canceled
import com.squareup.viewregistry.AlertContainerScreen
import com.squareup.viewregistry.BuilderBinding
import com.squareup.viewregistry.HandlesBack
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.buildView
import io.reactivex.Observable
import io.reactivex.Observable.never
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.reflect.jvm.jvmName

/**
 * A container view that can display a stream of [AlertContainerScreen]s using [AlertDialog]s.
 *
 * This view is back button friendly -- it implements [HandlesBack], delegating
 * to the view for the [AlertContainerScreen.baseScreen] if it implements that interface.
 */
class AlertContainer
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {
  private val base: View? get() = getChildAt(0)
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
    return (base as? HandlesBack)?.onBackPressed() == true
  }

  fun takeScreens(
    screens: Observable<out AlertContainerScreen<*>>,
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
          addView(it.viewForBase(screens, viewRegistry, this))
        }
    )

    // Compare the new modals list to the set of dialogs already on display. Show
    // any new ones, throw away any stale ones.
    takeScreensSubs.add(screens
        .whileAttached()
        .subscribe { containerScreen ->
          val newDialogs = mutableListOf<DialogRef>()
          for ((i, alert) in containerScreen.alerts.withIndex()) {
            newDialogs +=
              if (dialogs.size < i && dialogs[i].alert::class == alert::class) {
                dialogs[i]
              } else {
                DialogRef(alert, containerScreen.showDialog(i))
              }
          }

          (dialogs - newDialogs).forEach { it.dialog.hide() }
          dialogs = newDialogs
        }
    )
  }

  private fun showAlert(alertScreen: Alert): AlertDialog {
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
    val alert: Alert,
    val dialog: AlertDialog
  )

  private fun AlertContainerScreen<*>.showDialog(index: Int): AlertDialog = showAlert(alerts[index])

  companion object : ViewBinding<AlertContainerScreen<*>>
  by BuilderBinding(
      type = AlertContainerScreen::class.java,
      builder = { screens, builders, context, _ ->
        AlertContainer(context).apply {
          layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
          takeScreens(screens, builders)
        }
      }
  )
}

fun <T : Any> AlertContainerScreen<T>.viewForBase(
  containerScreens: Observable<out AlertContainerScreen<*>>,
  viewRegistry: ViewRegistry,
  container: ViewGroup
): View {
  val baseScreens: Observable<out T> = containerScreens.mapToBaseMatching(this)
  val binding: ViewBinding<T> = viewRegistry.getBinding(baseScreen::class.jvmName)
  return binding.buildView(baseScreens, viewRegistry, container)
}

fun <T : Any> Observable<out AlertContainerScreen<*>>.mapToBaseMatching(
  screen: AlertContainerScreen<T>
): Observable<out T> {
  return map { it.baseScreen }.ofType(screen.baseScreen::class.java)
}
