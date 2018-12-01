package com.squareup.sample.tictactoe.android

import com.squareup.viewbuilder.ViewBuilder.Registry

val TicTacToeViewBuilders = Registry(
    NewGameCoordinator, GamePlayCoordinator, GameOverCoordinator
)
