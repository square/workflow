package com.squareup.sample.dungeon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.Rect
import android.view.View
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.bindShowRendering
import kotlin.math.abs
import kotlin.math.min

/**
 * Large emoji used to determine how to scale the text paint size to fill the view.
 */
private const val EMOJI_FOR_MEASURE = "🌳"

/**
 * Custom view that can draw a [Board].
 */
class BoardView(context: Context) : View(context) {

  private val textPaint = Paint()
  private val glyphBounds = Rect()
  private var board: Board? = null
  private var cellWidth: Int = 0
  private var cellHeight: Int = 0
  private val fontMetrics = FontMetrics()

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    // Make ourselves square.
    val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
    val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)
    val minDimension = min(requestedWidth, requestedHeight)
    setMeasuredDimension(minDimension, minDimension)

    // Adjust text paint to fill if we've got a board to render.
    val board = this.board ?: return
    textPaint.getTextBounds(EMOJI_FOR_MEASURE, 0, 1, glyphBounds)
    textPaint.getFontMetrics(fontMetrics)
    cellWidth = minDimension / board.width
    cellHeight = minDimension / board.height

    // Emoji glyphs aren't perfectly square, so use the biggest dimension to scale.
    val widthFactor = cellWidth.toFloat() / glyphBounds.width().toFloat()
    val heightFactor = cellHeight.toFloat() / glyphBounds.height().toFloat()
    val scaleFactor = min(widthFactor, heightFactor)
    textPaint.textSize = textPaint.textSize * scaleFactor
  }

  override fun onDraw(canvas: Canvas) {
    val board = this.board ?: return

    for (x in 0 until board.width) {
      for (y in 0 until board.height) {
        val cell = board.cells[board.cellIndexOf(x, y)]
        val xPos = cellWidth * x.toFloat()
        // drawText's y parameter refers to the _baseline_ of the text to draw, so we need to offset
        // by the glyph height.
        val yPos = (cellHeight * y.toFloat()) + abs(fontMetrics.top)
        canvas.drawText(cell.toString(), xPos, yPos, textPaint)
      }
    }
  }

  private fun update(board: Board) {
    if (this.board != board) {
      this.board = board
      invalidate()
    }
  }

  @UseExperimental(ExperimentalWorkflowUi::class)
  companion object : ViewBinding<Board> by BuilderBinding(
      type = Board::class,
      viewConstructor = { _, initialRendering, contextForNewView, _ ->
        BoardView(contextForNewView)
            .apply { bindShowRendering(initialRendering, ::update) }
      })
}
