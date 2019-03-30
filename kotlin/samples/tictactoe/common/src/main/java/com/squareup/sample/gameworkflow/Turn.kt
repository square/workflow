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

import com.squareup.sample.gameworkflow.Player.X

/**
 * The state of a tic tac toe game. Also serves as the state of [TakeTurnsWorkflow].
 *
 * @param playing The symbol of the player whose turn it is.
 * @param board The current game board.
 */
data class Turn(
  val playing: Player = X,
  val board: Board = EMPTY_BOARD
) {

  private companion object {
    val EMPTY_BOARD = listOf(
        listOf(null, null, null),
        listOf(null, null, null),
        listOf(null, null, null)
    )
  }
}
