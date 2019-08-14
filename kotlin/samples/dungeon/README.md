# Dungeon Crawler Sample

This sample app is a simple 2D dungeon crawler game.

The game logic is all in the `common` module. The `app` module contains a simple Android app
that runs the game.

The core game logic is all defined by `GameWorkflow`. Each AI and player actor is implemented as a
child workflow of `GameWorkflow`. Each actor workflow renders its avatar, and outputs directions
to move. Actor workflows inject a `GameTicker` that they use to perform movement on a regular
cadence. The `GameWorkflow` gets movement events from all its children and updates the overall state
of the game to track locations and detect collisions.
