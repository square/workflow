package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.DungeonAppWorkflow.State.Loading
import com.squareup.sample.todo.R
import com.squareup.workflow.ui.LayoutRunner.Companion.bindNoRunner
import com.squareup.workflow.ui.ViewBinding

object LoadingBoardBinding : ViewBinding<Loading> by bindNoRunner(R.layout.loading_layout)
