/*
 * Copyright 2019 Square Inc.
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
package com.squareup.sample.hellocomposebinding

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Text
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.material.MaterialTheme
import androidx.ui.material.ripple.ripple
import com.squareup.workflow.ui.ViewRegistry

private val viewRegistry = ViewRegistry(HelloBinding)

class HelloBindingActivity : AppCompatActivity() {

  private var hello = true
  private val container by lazy { FrameLayout(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(container)

    println("[onCreate] Calling initial updateView($hello)…")
    updateView(hello) {
      println("[onCreate] Handling click…")
      onClick()
    }
  }

  private fun onClick() {
    hello = !hello
    println("[onClick] Calling updateView($hello)…")
    updateView(hello) {
      println("[onClick] Handling click…")
      onClick()
    }
  }

  private fun updateView(
    hello: Boolean,
    onClick: () -> Unit
  ) {
    println("[updateView] Setting container content ($hello)…")
    container.setContent {
      println("[updateView] Calling Content($hello)…")
      Content(hello) {
        println("[updateView] Handling click…")
        onClick()
      }
    }
  }
}

@Composable fun Content(
  hello: Boolean,
  onClick: () -> Unit
) {
  println("[Content] composing ($hello)")
  MaterialTheme {
    Clickable(
        modifier = Modifier.fillMaxSize()
            .ripple(bounded = true),
        onClick = onClick
    ) {
      Text(if (hello) "Hello" else "Goodbye", modifier = Modifier.wrapContentSize(Alignment.Center))
    }
  }
}
