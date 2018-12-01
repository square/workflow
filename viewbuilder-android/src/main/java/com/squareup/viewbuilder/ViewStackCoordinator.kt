package com.squareup.viewbuilder

import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.viewbuilder.ViewBuilder.Registry
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class ViewStackCoordinator(
  private val screens: Observable<out ViewStackScreen<*>>,
  private val builders: ViewBuilder.Registry
) : Coordinator() {
  private val subs = CompositeDisposable()

  override fun attach(view: View) {
    val viewStackLayout = view.findViewById<ViewStackFrameLayout>(
        R.id.view_stack
    )

    subs.add(screens.distinctUntilChanged { a, b -> a.key == b.key }
        .map { stackScreen -> stackScreen.buildWrappedView(screens, builders, viewStackLayout) }
        .subscribe { viewStackLayout.show(it) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  companion object : ViewBuilder<ViewStackScreen<*>> by LayoutViewBuilder(
      type = ViewStackScreen::class.java,
      layoutId = R.layout.view_stack_layout,
      coordinatorConstructor = ::ViewStackCoordinator
  )
}

fun <T : Any> ViewStackScreen<T>.buildWrappedView(
  screens: Observable<out ViewStackScreen<*>>,
  builders: Registry,
  container: ViewGroup
): View {
  val myScreens: Observable<out T> = screens.matchingWrappedScreens(this)
  val builder: ViewBuilder<T> = builders[key.type.name]
  return builder.buildView(myScreens, builders, container)
      .apply { betterKey = this@buildWrappedView.key }
}

var View.betterKey: ViewStackKey<*>
  get() {
    return getTag(R.id.view_stack_key) as ViewStackKey<*>?
        ?: throw IllegalArgumentException("No key found on $this")
  }
  set(screenKey) = setTag(R.id.view_stack_key, screenKey)
