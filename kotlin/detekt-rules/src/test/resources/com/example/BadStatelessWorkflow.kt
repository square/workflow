package com.example

import com.squareup.workflow

class BadStatelessWorkflow : StatefulWorkflow<String, String, String, String>() {
  override fun initialState(
    props: String,
    snapshot: Snapshot?
  ): String = ""

  override fun render(
    props: String,
    state: String,
    context: RenderContext<String, String>
  ): String {
    action {
      nextState = state
    }
    return ""
  }

  override fun snapshotState(state: String): Snapshot = Snapshot.EMPTY
}
