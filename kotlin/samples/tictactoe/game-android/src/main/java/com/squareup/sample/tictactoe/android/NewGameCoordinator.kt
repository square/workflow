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

import android.view.View
import android.widget.Button
import android.widget.EditText
import com.squareup.coordinators.Coordinator
import com.squareup.sample.tictactoe.NewGameScreen
import com.squareup.sample.tictactoe.RunGameEvent
import com.squareup.sample.tictactoe.RunGameEvent.NoMore
import com.squareup.sample.tictactoe.RunGameEvent.StartGame
import com.squareup.viewregistry.LayoutBinding
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.setBackHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class NewGameCoordinator(
  private val screens: Observable<out NewGameScreen>
) : Coordinator() {

  private val subs = CompositeDisposable()

  private lateinit var playerX: EditText
  private lateinit var playerO: EditText
  private lateinit var button: Button

  override fun attach(view: View) {
    super.attach(view)

    playerX = view.findViewById(R.id.player_X)
    playerO = view.findViewById(R.id.player_O)
    button = view.findViewById(R.id.start_game)

    subs.add(screens.map { it.onEvent }
        .subscribe { this.update(view, it) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  private fun update(
    view: View,
    onEvent: (RunGameEvent) -> Unit
  ) {
    button.setOnClickListener {
      onEvent(
          StartGame(
              playerX.text.toString(),
              playerO.text.toString()
          )
      )
    }

    view.setBackHandler { onEvent(NoMore) }
  }

  companion object : ViewBinding<NewGameScreen> by LayoutBinding.of(
      R.layout.new_game_layout, ::NewGameCoordinator
  )
}
