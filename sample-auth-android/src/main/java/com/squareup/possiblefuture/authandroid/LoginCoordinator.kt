package com.squareup.possiblefuture.authandroid

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.possiblefuture.authworkflow.LoginScreen
import com.squareup.possiblefuture.authworkflow.SubmitLogin
import com.squareup.viewbuilder.LayoutViewBuilder
import com.squareup.viewbuilder.ViewBuilder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class LoginCoordinator(private val screens: Observable<out LoginScreen>) : Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var error: TextView
  private lateinit var email: EditText
  private lateinit var password: EditText
  private lateinit var button: Button

  override fun attach(view: View) {
    super.attach(view)

    error = view.findViewById(R.id.login_error_message)
    email = view.findViewById(R.id.login_email)
    password = view.findViewById(R.id.login_password)
    button = view.findViewById(R.id.login_button)

    subs.add(screens.subscribe(this::update))
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  private fun update(screen: LoginScreen) {
    error.text = screen.data

    button.setOnClickListener {
      screen.onEvent(SubmitLogin(email.text.toString(), password.text.toString()))
    }
  }

  companion object : ViewBuilder<LoginScreen> by LayoutViewBuilder.of(
      R.layout.login_layout, ::LoginCoordinator
  )
}
