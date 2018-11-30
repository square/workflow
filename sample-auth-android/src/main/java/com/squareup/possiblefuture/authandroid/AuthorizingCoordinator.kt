package com.squareup.possiblefuture.authandroid

import android.view.View
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.possiblefuture.authworkflow.AuthorizingScreen
import com.squareup.viewbuilder.LayoutViewBuilder
import com.squareup.viewbuilder.ViewBuilder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class AuthorizingCoordinator(private val screens: Observable<out AuthorizingScreen>) :
    Coordinator() {
  private val subs = CompositeDisposable()

  override fun attach(view: View) {
    super.attach(view)
    val messageView = view.findViewById<TextView>(R.id.authorizing_message)
    subs.add(screens.map { s -> s.message }
        .subscribe { messageView.text = it })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  companion object : ViewBuilder<AuthorizingScreen> by LayoutViewBuilder.of(
      R.layout.authorizing_layout, ::AuthorizingCoordinator
  )
}
