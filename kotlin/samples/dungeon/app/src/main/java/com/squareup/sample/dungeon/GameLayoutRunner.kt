package com.squareup.sample.dungeon

import android.view.View
import android.view.ViewGroup
import com.squareup.sample.dungeon.GameEvent.MoveDown
import com.squareup.sample.dungeon.GameEvent.MoveLeft
import com.squareup.sample.dungeon.GameEvent.MoveRight
import com.squareup.sample.dungeon.GameEvent.MoveUp
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

  override fun showRendering(rendering: GameRendering) {
    if (!::boardView.isInitialized) {
      boardView = viewRegistry.buildView(rendering.board, boardContainer)
      boardContainer.addView(boardView)
    } else {
      boardView.showRendering(rendering.board)
    }

    moveLeft.setOnClickListener { rendering.onEvent(MoveLeft) }
    moveRight.setOnClickListener { rendering.onEvent(MoveRight) }
    moveUp.setOnClickListener { rendering.onEvent(MoveUp) }
    moveDown.setOnClickListener { rendering.onEvent(MoveDown) }
  }

  companion object : ViewBinding<GameRendering> by bind(
      R.layout.game_layout, ::GameLayoutRunner
  )
}
