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
package com.squareup.sample.gameworkflow

import android.view.View
import android.widget.Button
import android.widget.EditText
import com.squareup.sample.gameworkflow.NewGameScreen.Event.CancelNewGame
import com.squareup.sample.gameworkflow.NewGameScreen.Event.StartGame
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.setBackHandler

@UseExperimental(ExperimentalWorkflowUi::class)
internal class NewGameLayoutRunner(private val view: View) : LayoutRunner<NewGameScreen> {

  private val playerX: EditText = view.findViewById(R.id.player_X)
  private val playerO: EditText = view.findViewById(R.id.player_O)
  private val button: Button = view.findViewById(R.id.start_game)

  override fun showRendering(rendering: NewGameScreen) {
    if (playerX.text.isBlank()) playerX.setText(rendering.defaultNameX)
    if (playerO.text.isBlank()) playerO.setText(rendering.defaultNameO)

    button.setOnClickListener {
      rendering.onEvent(StartGame(playerX.text.toString(), playerO.text.toString()))
    }

    view.setBackHandler { rendering.onEvent(CancelNewGame) }
  }

  companion object : ViewBinding<NewGameScreen> by bind(
      R.layout.new_game_layout, ::NewGameLayoutRunner
  )
}
