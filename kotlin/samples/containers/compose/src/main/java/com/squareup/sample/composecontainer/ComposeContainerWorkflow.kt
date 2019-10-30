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
package com.squareup.sample.composecontainer

import com.squareup.sample.composecontainer.pictureframe.PictureFrameRendering
import com.squareup.sample.composecontainer.pictures.PicturesWorkflow
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.renderChild

/**
 * Root workflow for this sample. Wraps renderings from a [PicturesWorkflow] in
 * [PictureFrameRendering]s.
 */
object ComposeContainerWorkflow : StatelessWorkflow<Unit, Nothing, PictureFrameRendering>() {
  override fun render(
    props: Unit,
    context: RenderContext<Nothing, Nothing>
  ): PictureFrameRendering = PictureFrameRendering(
      contents = context.renderChild(PicturesWorkflow)
  )
}
