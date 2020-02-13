package com.example

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow

/**
 * TODO write documentation
 */
class BadStatefulWorkflow : StatefulWorkflow<String, String, String, String>() {
  override fun initialState(
    props: String,
    snapshot: Snapshot?
  ): String {
    TODO("not implemented")
  }

  override fun render(
    props: String,
    state: String,
    context: RenderContext<String, String>
  ): String {
    TODO("not implemented")
  }

  override fun snapshotState(state: String): Snapshot {
    TODO("not implemented")
  }
}
