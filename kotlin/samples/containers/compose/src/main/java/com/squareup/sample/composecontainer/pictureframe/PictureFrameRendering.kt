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
package com.squareup.sample.composecontainer.pictureframe

import androidx.annotation.DimenRes
import com.squareup.sample.composecontainer.R

/**
 * A rendering that draws a picture frame around its [contents] using [PictureFrame].
 *
 * @param contents The rendering to show in the frame. Must have a binding in the `ViewRegistry`.
 */
data class PictureFrameRendering(
  val contents: Any,
  @DimenRes val thicknessRes: Int = R.dimen.default_picture_frame_thickness
)
