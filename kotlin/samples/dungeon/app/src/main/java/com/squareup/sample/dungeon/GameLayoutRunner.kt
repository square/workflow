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
package com.squareup.sample.dungeon

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_UP
import android.view.View
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.GameWorkflow.GameRendering
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.WorkflowViewStub

/**
 * Renders a live game, including the board with player and actors, and the buttons to control
 * the player.
 */
class GameLayoutRunner(view: View) : LayoutRunner<GameRendering> {

  private val boardView: WorkflowViewStub = view.findViewById(R.id.board_stub)
  private val moveLeft: View = view.findViewById(R.id.move_left)
  private val moveRight: View = view.findViewById(R.id.move_right)
  private val moveUp: View = view.findViewById(R.id.move_up)
  private val moveDown: View = view.findViewById(R.id.move_down)

  private lateinit var rendering: GameRendering

  init {
    moveLeft.registerPlayerEventHandlers(LEFT)
    moveRight.registerPlayerEventHandlers(RIGHT)
    moveUp.registerPlayerEventHandlers(UP)
    moveDown.registerPlayerEventHandlers(DOWN)
  }

  override fun showRendering(
    rendering: GameRendering,
    viewEnvironment: ViewEnvironment
  ) {
    boardView.update(rendering.board, viewEnvironment)
    this.rendering = rendering

    // Disable the views if we don't have an event handler, e.g. when the game has finished.
    val controlsEnabled = !rendering.gameOver
    moveLeft.isEnabled = controlsEnabled
    moveRight.isEnabled = controlsEnabled
    moveUp.isEnabled = controlsEnabled
    moveDown.isEnabled = controlsEnabled
  }

  private fun View.registerPlayerEventHandlers(direction: Direction) {
    setOnTouchListener { _, motionEvent ->
      when (motionEvent.action and ACTION_MASK) {
        ACTION_DOWN -> rendering.onStartMoving(direction)
        ACTION_UP -> rendering.onStopMoving(direction)
      }
      // Always return false, so the button ripples and animates correctly.
      return@setOnTouchListener false
    }
  }

  companion object : ViewFactory<GameRendering> by bind(
      R.layout.game_layout, ::GameLayoutRunner
  )
}
