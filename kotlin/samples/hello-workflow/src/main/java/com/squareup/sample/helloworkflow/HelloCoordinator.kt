package com.squareup.sample.helloworkflow

import android.view.View
import android.widget.SimpleAdapter.ViewBinder
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.sample.helloworkflow.HelloWorkflow.Rendering
import com.squareup.workflow.ui.LayoutBinding
import com.squareup.workflow.ui.ViewBinding
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

@Suppress("EXPERIMENTAL_API_USAGE")
class HelloCoordinator(
  private val renderings: Observable<out Rendering>
) : Coordinator() {
  private val subs = CompositeDisposable()

  override fun attach(view: View) {
    val messageView = view.findViewById<TextView>(R.id.hello_message)

    subs.add(renderings.subscribe { rendering ->
      messageView.text = rendering.message
      messageView.setOnClickListener { rendering.onClick(Unit) }
    })
  }

  override fun detach(view: View) {
    super.detach(view)
  }

  companion object : ViewBinding<Rendering> by LayoutBinding.of(
      R.layout.hello_layout, ::HelloCoordinator
  )
}
