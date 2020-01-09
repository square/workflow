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
import com.squareup.cycler.Recycler
import com.squareup.cycler.toDataSource
import com.squareup.sample.dungeon.DungeonAppWorkflow.DisplayBoardsListScreen
import com.squareup.sample.dungeon.board.Board
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.WorkflowViewStub

/**
 * TODO write documentation
 */
class BoardsListLayoutRunner(view: View) : LayoutRunner<DisplayBoardsListScreen> {

  private var onBoardSelected: (index: Int) -> Unit = {}

  private val recycler =
    Recycler.adopt<Pair<Board, ContainerHints>>(view.findViewById(R.id.boards_list_recycler)) {
      row<Pair<Board, ContainerHints>, ViewGroup> {
        create(R.layout.boards_list_item) {
          bind { index, (board, containerHints) ->
            val card: CardView = this.view.findViewById(R.id.board_card)
            val boardNameView: TextView = this.view.findViewById(R.id.board_name)
            val boardPreviewView: WorkflowViewStub = this.view.findViewById(R.id.board_preview)

            boardNameView.text = board.metadata.name
            boardPreviewView.update(board, containerHints)
            card.setOnClickListener { onBoardSelected(index) }
          }
        }
      }
    }

  override fun showRendering(
    rendering: DisplayBoardsListScreen,
    containerHints: ContainerHints
  ) {
    // Associate the containerHints to each item so it can be used when binding the RecyclerView
    // item.
    recycler.data = rendering.boards
        .map { it to containerHints }
        .toDataSource()
    onBoardSelected = rendering.onBoardSelected
  }

  companion object : ViewBinding<DisplayBoardsListScreen> by bind(
      R.layout.boards_list_layout, ::BoardsListLayoutRunner
  )
}
