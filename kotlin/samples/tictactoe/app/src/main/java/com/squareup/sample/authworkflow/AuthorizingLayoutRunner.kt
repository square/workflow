/*
 * Copyright 2017 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.authworkflow

import android.view.View
import android.widget.TextView
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.Hints
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

internal class AuthorizingLayoutRunner(view: View) : LayoutRunner<AuthorizingScreen> {
  private val messageView: TextView = view.findViewById(R.id.authorizing_message)

  override fun showRendering(
    rendering: AuthorizingScreen,
    hints: Hints
  ) {
    messageView.text = rendering.message
  }

  companion object : ViewBinding<AuthorizingScreen> by bind(
      R.layout.authorizing_layout, ::AuthorizingLayoutRunner
  )
}
