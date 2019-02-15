package com.squareup.viewregistry

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.reflect.jvm.jvmName

/**
 * Base class for containers that show [HasModals.modals] in [Dialog]s
 */
abstract class AbstractModalContainer<M : Any>
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet), HandlesBack {
  private val base: View? get() = getChildAt(0)
  private var dialogs: List<DialogRef<M>> = emptyList()
  private val takeScreensSubs = CompositeDisposable()

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
          addView(it.viewForBase(screens, viewRegistry, this))
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
          dialogs = newDialogs
        }
    )
  }

  /**
   * Returns true if the new screen can be shown by the dialog that was created for the receiver.
   * Default implementation compares equality of the receiver and the new screen.
   */
  protected open fun M.matches(nextModal: M): Boolean = this == nextModal

  protected abstract fun showDialog(
    modalScreen: M,
    screens: Observable<out M>,
    viewRegistry: ViewRegistry
  ): Dialog

  private fun HasModals<*, M>.showDialog(
    index: Int,
    screens: Observable<out M>,
    viewRegistry: ViewRegistry
  ): Dialog = showDialog(modals[index], screens, viewRegistry)

  private fun <T> Observable<T>.whileAttached(): Observable<T> =
    attached.switchMap { isAttached -> if (isAttached) this else Observable.never<T>() }

  protected class DialogRef<M>(
    val screen: M,
    val dialog: Dialog
  )

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

  protected fun <M : Any> HasModals<*, M>.viewForModal(
    containerScreens: Observable<out HasModals<*, *>>,
    index: Int,
    viewRegistry: ViewRegistry,
    container: ViewGroup
  ): View {
    if (index < modals.size) {
      throw IndexOutOfBoundsException(
          "index $index into $this.modals must be less than ${modals.size}"
      )
    }
    val modalScreens: Observable<out M> = containerScreens.mapToModalMatching(this, index)
    val binding: ViewBinding<M> = viewRegistry.getBinding(baseScreen::class.jvmName)
    return binding.buildView(modalScreens, viewRegistry, container)
  }

  private fun <M : Any> Observable<out HasModals<*, *>>.mapToModalMatching(
    screen: HasModals<*, M>,
    index: Int
  ): Observable<out M> = filter { index < it.modals.size }
      .map { it.modals[index] }
      .ofType(screen.modals[index]::class.java)
}
