package com.squareup.possiblefuture.shell

import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.viewbuilder.HandlesBack.Helper.onBackPressed
import com.squareup.viewbuilder.setBackHandler
import com.squareup.workflow.AnyScreen
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class LogoutCoordinator(
  private val screens: Observable<out AnyScreen>,
  private val onLogout: () -> Unit
) : Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var logoutButton: View
  private lateinit var frame: ViewGroup

  override fun attach(view: View) {
    super.attach(view)
    logoutButton = view.findViewById(R.id.logout_button)
    frame = view.findViewById(R.id.logout_decorator_body)

    subs.add(screens
        .subscribe {
          // Give the wrapped view a chance to handle back button events. If it declines,
          // log out.
          view.setBackHandler {
            if (!onBackPressed(frame.getChildAt(0))) onLogout()
          }
          logoutButton.setOnClickListener { onLogout() }
        })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }
}
