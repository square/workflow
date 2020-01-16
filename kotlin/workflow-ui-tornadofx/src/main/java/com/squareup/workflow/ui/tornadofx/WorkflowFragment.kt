package com.squareup.workflow.ui.tornadofx

import tornadofx.Fragment

/**
 * TODO write javadoc.
 */
abstract class WorkflowFragment<RenderingT>(title: String? = null) : Fragment(title) {
  val rendering: SimpleObjectProperty<RenderingT> = TODO()
}
