package com.squareup.sample.poetryapp

import com.squareup.workflow.ui.Named
import com.squareup.workflow.ui.backstack.BackStackScreen
import com.squareup.workflow.ui.compose.bindCompose
import com.squareup.workflow.ui.compose.showRendering
import com.zachklipp.compose.backstack.Backstack
import com.zachklipp.compose.backstack.InspectionGestureDetector

/**
 * TODO write documentation
 */
val ComposeBackstackContainer = bindCompose<BackStackScreen<*>> { rendering, ve ->
  // TODO do we even need this?
  val named: BackStackScreen<Named<*>> = rendering
      // ViewStateCache requires that everything be Named.
      // It's fine if client code is already using Named for its own purposes, recursion works.
      .map { Named(it, "backstack") }

  InspectionGestureDetector(true) { inspectionParams ->
    println("frames: ${named.frames}")
    Backstack(named.frames, inspectionParams = inspectionParams) { screen ->
      ve.showRendering(screen.wrapped)
    }
  }
}
