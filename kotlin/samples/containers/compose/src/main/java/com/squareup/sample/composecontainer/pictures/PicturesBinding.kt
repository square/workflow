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
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry", "UNUSED_VALUE")

package com.squareup.sample.composecontainer.pictures

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import com.squareup.sample.composecontainer.pictures.PicturesWorkflow.Rendering
import com.squareup.workflow.ui.compose.bindCompose
import com.squareup.workflow.ui.compose.tooling.preview

val PicturesBinding = bindCompose<Rendering> { rendering, _ ->
  SlideIn(rendering.pictureUrl) { url ->
    CoilImage(
        url = url,
        drawLoading = {
          Box(modifier = LayoutPadding(200.dp), gravity = Alignment.Center) {
            CircularProgressIndicator()
          }
        },
        drawLoaded = { drawImage ->
          Clickable(onClick = rendering.onTap) {
            drawImage()
          }
        }
    )
  }
}

@Preview
@Composable fun PicturesBindingPreview() {
  MaterialTheme {
    Surface {
      PicturesBinding.preview(
          Rendering(
              pictureUrl = CoilPreviewUrl,
              pictureDescription = "The Picture",
              onTap = {}
          )
      )
    }
  }
}
