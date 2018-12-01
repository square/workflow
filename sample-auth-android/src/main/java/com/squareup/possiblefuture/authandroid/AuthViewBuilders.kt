package com.squareup.possiblefuture.authandroid

import com.squareup.viewbuilder.ViewBuilder.Registry

val AuthViewBuilders = Registry(
    AuthorizingCoordinator, LoginCoordinator, SecondFactorCoordinator
)
