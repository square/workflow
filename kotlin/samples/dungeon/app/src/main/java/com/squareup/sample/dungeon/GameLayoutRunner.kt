package com.squareup.sample.dungeon

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.PlayerWorkflow.Event.StartMoving
import com.squareup.sample.dungeon.PlayerWorkflow.Event.StopMoving
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.showRendering

@UseExperimental(ExperimentalWorkflowUi::class)
class GameLayoutRunner(
  view: View,
  private val viewRegistry: ViewRegistry
) : LayoutRunner<GameRendering> {

  private val boardContainer: ViewGroup = view.findViewById(R.id.board_container)
  private lateinit var boardView: View
  private val moveLeft: View = view.findViewById(R.id.move_left)
  private val moveRight: View = view.findViewById(R.id.move_right)
  private val moveUp: View = view.findViewById(R.id.move_up)
  private val moveDown: View = view.findViewById(R.id.move_down)

  private var playerEvent: (PlayerWorkflow.Event) -> Unit = {}

  init {
    moveLeft.registerPlayerEventHandlers(LEFT)
    moveRight.registerPlayerEventHandlers(RIGHT)
    moveUp.registerPlayerEventHandlers(UP)
    moveDown.registerPlayerEventHandlers(DOWN)
  }

  override fun showRendering(rendering: GameRendering) {
    if (!::boardView.isInitialized) {
      boardView = viewRegistry.buildView(rendering.board, boardContainer)
      boardContainer.addView(boardView)
    } else {
      boardView.showRendering(rendering.board)
    }

    playerEvent = rendering.player.onEvent
  }

  private fun View.registerPlayerEventHandlers(direction: Direction) {
    setOnTouchListener { _, motionEvent ->
      when (motionEvent.action and ACTION_MASK) {
        ACTION_DOWN -> playerEvent(StartMoving(direction))
        ACTION_UP -> playerEvent(StopMoving(direction))
      }
      // Always return false, so the button ripples and animates correctly.
      return@setOnTouchListener false
    }
  }

  companion object : ViewBinding<GameRendering> by bind(
      R.layout.game_layout, ::GameLayoutRunner
  )
}
