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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.squareup.sample.container.SampleContainers
import com.squareup.sample.porchbox.PorchWorkflow.Auth
import com.squareup.sample.porchbox.PorchWorkflow.CloseInbox
import com.squareup.sample.porchbox.PorchWorkflow.RealtimeDB
import com.squareup.sample.porchbox.view.PorchboxViews
import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener
import com.squareup.workflow.ui.WorkflowRunner
import com.squareup.workflow.ui.backstack.BackStackContainer
import com.squareup.workflow.ui.modal.AlertContainer
import com.squareup.workflow.ui.plus
import com.squareup.workflow.ui.setContentWorkflow
import timber.log.Timber

private val viewRegistry = SampleContainers + PorchboxViews + BackStackContainer + AlertContainer

class PorchboxActivity : AppCompatActivity() {

  private lateinit var auth: FirebaseAuth

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    auth = FirebaseAuth.getInstance()
    Picasso.get()
        .setIndicatorsEnabled(BuildConfig.DEBUG)
    Picasso.get().isLoggingEnabled = BuildConfig.DEBUG

    // The interface defaults for [Auth] and [RealtimeDB] are sufficient
    setContentWorkflow(
        registry = viewRegistry,
        configure = {
          WorkflowRunner.Config<PorchWorkflow.Props, CloseInbox>(
              PorchWorkflow(object : Auth {}, object: RealtimeDB {}),
              PorchWorkflow.Props,
              diagnosticListener = object : SimpleLoggingDiagnosticListener() {
                override fun println(text: String) = Timber.v(text)
              }
          )
        },
        onResult = { finish() }
    )
  }

  companion object {
    init {
      Timber.plant(Timber.DebugTree())
    }
  }
}