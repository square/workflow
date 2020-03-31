/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.porchbox

import com.squareup.sample.porchbox.LoginWorkflow.LoginCredentials
import com.squareup.sample.porchbox.LoginWorkflow.Props
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.makeEventSink

/**
 * Simple Workflow that manages getting the Login screen rendered and passing the credentials
 * to the [PorchWorkflow] to do the logging in. No error handling for now.
 */
object LoginWorkflow : StatelessWorkflow<Props, LoginCredentials, LoginRendering>() {
  object Props

  data class LoginCredentials(
    val email: String,
    val pass: String
  )

  override fun render(
    props: Props,
    context: RenderContext<Nothing, LoginCredentials>
  ): LoginRendering {
    val sink: Sink<LoginCredentials> = context.makeEventSink { setOutput(it) }

    val onLogin: ((String, String) -> Unit) = { email, password ->
      sink.send(LoginCredentials(email, password))
    }

    return LoginRendering(
        onLogin = onLogin
    )
  }
}

data class LoginRendering(
  val onLogin: ((String, String) -> Unit)
)