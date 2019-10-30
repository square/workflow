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
package com.squareup.sample.composecontainer.pictures

import com.squareup.sample.composecontainer.pictures.PicturesWorkflow.Rendering
import com.squareup.sample.composecontainer.pictures.PicturesWorkflow.State
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action

/**
 * Workflow that shows a set of pictures from the internet, and cycles through them when tapped.
 */
object PicturesWorkflow : StatefulWorkflow<Unit, State, Nothing, Rendering>() {

  enum class Picture(val url: String) {
    Terrier("https://free-images.com/md/b436/1yorkshire_terrier_171701_640.jpg"),
    Coffee("https://free-images.com/md/86a4/aroma_aromatic_beverage_bio.jpg"),
    ;

    val next: Picture get() = values()[(ordinal + 1) % values().size]
  }

  data class State(val picture: Picture)

  data class Rendering(
    val pictureUrl: String,
    val pictureDescription: String,
    val onTap: () -> Unit
  )

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = State(Picture.values().first())

  override fun render(
    props: Unit,
    state: State,
    context: RenderContext<State, Nothing>
  ): Rendering = Rendering(
      pictureUrl = state.picture.url,
      pictureDescription = state.picture.name,
      onTap = { context.actionSink.send(showNextPicture()) }
  )

  override fun snapshotState(state: State): Snapshot = Snapshot.of("")

  private fun showNextPicture() = action {
    nextState = nextState.copy(picture = nextState.picture.next)
  }
}
