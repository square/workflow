package com.squareup.sample.dungeon

import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.gridlayout.widget.GridLayout
import androidx.gridlayout.widget.GridLayout.spec
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

@UseExperimental(ExperimentalWorkflowUi::class)
class BoardLayoutRunner(view: View) : LayoutRunner<BoardRendering> {

  private val board: GridLayout = view.findViewById(R.id.board)
  private val cells = mutableListOf<TextView>()
  private var width: Int = 0
  private var height: Int = 0

  override fun showRendering(rendering: BoardRendering) {
    if (width != rendering.width || height != rendering.height) {
      width = rendering.width
      height = rendering.height
      clearAndResize()
    }

    for (i in rendering.cells.indices) {
      cells[i].text = rendering.cells[i].toString()
    }
  }

  private fun clearAndResize() {
    board.removeAllViews()
    cells.clear()

    for (x in 0 until width) {
      for (y in 0 until height) {
        val cell = TextView(board.context)
        // TODO get the grid to fill up available space
        val params = GridLayout.LayoutParams(spec(y), spec(x))
        board.addView(cell, params)
        cells += cell
      }
    }
  }

  companion object : ViewBinding<BoardRendering> by bind(
      R.layout.dungeon_view, ::BoardLayoutRunner
  )
}
