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
package com.squareup.sample.gameworkflow

import com.squareup.sample.gameworkflow.Player.O
import com.squareup.sample.gameworkflow.Player.X

/**
 * The X's and O's of a Tic Tac Toe game.
 */
enum class Player {
  X,
  O
}

val Player.other: Player
  get() = when (this) {
    X -> O
    O -> X
  }

fun Player.name(playerInfo: PlayerInfo) = when (this) {
  X -> playerInfo.xName
  O -> playerInfo.oName
}
