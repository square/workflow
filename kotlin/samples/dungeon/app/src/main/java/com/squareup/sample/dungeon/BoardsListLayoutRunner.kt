/*
 * Copyright 2020 Square Inc.
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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.squareup.cycler.DataSource
import com.squareup.cycler.Recycler
import com.squareup.cycler.toDataSource
import com.squareup.sample.dungeon.DungeonAppWorkflow.DisplayBoardsListScreen
import com.squareup.sample.dungeon.board.Board
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.WorkflowViewStub

/**
 * Renders a list of boards in a grid, along with previews of the boards.
 *
 * Notably, this runner uses the [Cycler](https://github.com/square/cycler) library to configure
 * a `RecyclerView`.
 */
class BoardsListLayoutRunner(rootView: View) : LayoutRunner<DisplayBoardsListScreen> {

  /**
   * Used to associate a single [ViewEnvironment] and [DisplayBoardsListScreen.onBoardSelected]
   * event handler with every item of a [DisplayBoardsListScreen].
   *
   * @see toDataSource
   */
  private data class BoardItem(
    val board: Board,
    val viewEnvironment: ViewEnvironment,
    val onClicked: () -> Unit
  )

  private val recycler =
    Recycler.adopt<BoardItem>(rootView.findViewById(R.id.boards_list_recycler)) {
      // This defines how to instantiate and bind a row of a particular item type.
      row<BoardItem, ViewGroup> {
        // This defines how to inflate and bind the layout for this row type.
        create(R.layout.boards_list_item) {
          // And this is the actual binding logic.
          bind { _, item ->
            val card: CardView = view.findViewById(R.id.board_card)
            val boardNameView: TextView = view.findViewById(R.id.board_name)
            // The board preview is actually rendered using the same LayoutRunner as the actual
            // live game. It's easy to delegate to it by just putting a WorkflowViewStub in our
            // layout and giving it the Board.
            val boardPreviewView: WorkflowViewStub = view.findViewById(R.id.board_preview)

            boardNameView.text = item.board.metadata.name
            boardPreviewView.update(item.board, item.viewEnvironment)
            card.setOnClickListener { item.onClicked() }
          }
        }
      }
    }

  override fun showRendering(
    rendering: DisplayBoardsListScreen,
    viewEnvironment: ViewEnvironment
  ) {
    // Associate the containerHints and event handler to each item because it needs to be used when
    // binding the RecyclerView item above.
    // Recycler is configured with a DataSource, which effectively (and often in practice) a simple
    // wrapper around a List.
    recycler.data = rendering.toDataSource(viewEnvironment)
  }

  /**
   * Converts this [DisplayBoardsListScreen] into a [DataSource] by lazily wrapping it in a
   * [BoardItem] to associate it with the [ViewEnvironment] and selection event handler from the
   * rendering.
   */
  private fun DisplayBoardsListScreen.toDataSource(
    viewEnvironment: ViewEnvironment
  ): DataSource<BoardItem> = object : DataSource<BoardItem> {
    override val size: Int get() = boards.size

    override fun get(i: Int): BoardItem = BoardItem(
        board = boards[i],
        viewEnvironment = viewEnvironment,
        onClicked = { onBoardSelected(i) }
    )
  }

  companion object : ViewFactory<DisplayBoardsListScreen> by bind(
      R.layout.boards_list_layout, ::BoardsListLayoutRunner
  )
}
