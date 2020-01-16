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

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Text
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.material.ripple.ripple
import androidx.ui.tooling.preview.Preview
import com.squareup.sample.hellocomposebinding.HelloWorkflow.Rendering
import com.squareup.workflow.ui.compose.bindCompose

val HelloBinding = bindCompose<Rendering> { rendering, _ ->
  DrawHelloRendering(rendering)
}

@Composable
private fun DrawHelloRendering(rendering: Rendering) {
  Clickable(
      modifier = Modifier.fillMaxSize()
          .ripple(bounded = true),
      onClick = { rendering.onClick() }
  ) {
    Text(rendering.message, modifier = Modifier.wrapContentSize(Alignment.Center))
  }
}

@Preview(heightDp = 150)
@Composable
fun DrawHelloRenderingPreview() {
  MaterialTheme {
    Surface {
      DrawHelloRendering(Rendering("Hello!", onClick = {}))
    }
  }
}
