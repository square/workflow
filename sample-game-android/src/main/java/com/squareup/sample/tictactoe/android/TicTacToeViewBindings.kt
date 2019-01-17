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
package com.squareup.sample.tictactoe.android

import com.squareup.sample.tictactoe.GameOverScreen
import com.squareup.sample.tictactoe.GamePlayScreen
import com.squareup.viewregistry.NoEffect
import com.squareup.viewregistry.ViewRegistry

val TicTacToeViewBindings = ViewRegistry(
    NewGameCoordinator, GamePlayCoordinator, GameOverCoordinator
) + NoEffect(from = GamePlayScreen::class, to = GameOverScreen::class)
