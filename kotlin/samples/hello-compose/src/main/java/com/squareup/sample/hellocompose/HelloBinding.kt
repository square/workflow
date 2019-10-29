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
package com.squareup.sample.hellocompose

import androidx.compose.Composable
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.layout.Center
import androidx.ui.material.MaterialTheme
import androidx.ui.material.themeTextStyle
import androidx.ui.tooling.preview.Preview
import com.squareup.sample.hellocompose.HelloWorkflow.Rendering
import com.squareup.workflow.ui.compose.bindCompose

// Can't use a function reference because of what looks like a compiler bug.
val HelloBinding = bindCompose<Rendering> { showHello(it) }

@Composable
private fun showHello(rendering: Rendering) {
  MaterialTheme {
    Clickable(onClick = rendering.onClick) {
      Center {
        Text(rendering.message, style = +themeTextStyle { h1 })
      }
    }
  }
}

@Preview
@Composable
fun showHelloPreview() {
  showHello(Rendering("Hello!", onClick = {}))
}
