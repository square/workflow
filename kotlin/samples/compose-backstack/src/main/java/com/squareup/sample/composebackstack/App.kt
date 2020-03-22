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
@file:Suppress(
    "RemoveEmptyParenthesesFromAnnotationEntry",
    "RemoveForLoopIndices",
    "UNUSED_VARIABLE"
)

package com.squareup.sample.composebackstack

import android.os.Handler
import androidx.compose.Composable
import androidx.compose.Pivotal
import androidx.compose.onActive
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.foundation.Box
import androidx.ui.foundation.DrawBorder
import androidx.ui.graphics.Color
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.material.Button
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp

enum class BackstackImpl {
  Fader,
  Slider
}

//private val screens = listOf(
//    "Main Screen\n\n",
//    "\nSecond Screen\n",
//    "\n\nThird Screen"
//)

private val backstacks = listOf(
    listOf("one"),
    listOf("one", "two"),
    listOf("one", "two", "three")
).associateBy { it.joinToString() }

private val backstackNames = backstacks.keys.sorted()

@Composable
fun App() {
  var currentBackstack by state { backstackNames.first() }

  Column {
    Text("Current backstack: $currentBackstack")
    backstackNames.forEach { name ->
      Button(onClick = { currentBackstack = name }) { Text(name) }
    }
    Box(
        modifier = LayoutSize.Fill +
            LayoutPadding(24.dp) +
            DrawBorder(size = 3.dp, color = Color.Red)
    ) {
      Backstack(backstack = backstacks.getValue(currentBackstack)) {
        DrawScreen(it)
      }
    }
  }
}

@Composable
private fun DrawScreen(key: String) {
  onActive {
    println("Screen onActive: $key")
    onDispose {
      println("Screen onDispose: $key")
    }
  }
  Center {
    Text("$key: ${Counter(200)}")
  }
}

@Suppress("SameParameterValue")
@Composable
private fun Counter(@Pivotal periodMs: Long): Int {
  var value by state { 0 }
  onActive {
    val mainHandler = Handler()
    var disposed = false
    onDispose { disposed = true }
    fun schedule() {
      mainHandler.postDelayed({
        value++
        if (!disposed) schedule()
      }, periodMs)
    }
    schedule()
  }
  return value
}

//@Composable fun App() {
//  var selectedBackstackImpl by state { Fader }
//
//  val scrollPosition = remember { SliderPosition(initial = 1f) }
//  Column(modifier = LayoutSize.Fill) {
//    Spinner(
//        items = BackstackImpl.values().asList(),
//        selectedIndex = selectedBackstackImpl.ordinal,
//        onSelected = { selectedBackstackImpl = BackstackImpl.values()[it] }
//    ) {
//      ListItem(text = it.toString())
//    }
//
//    Box(modifier = LayoutFlexible(1f)) {
//      when (selectedBackstackImpl) {
//        Fader -> {
//          BackstackFader(
//              screenList = screens,
//              scrollPosition = scrollPosition.value
//          ) {
//            AppScreen(name = it)
//          }
//        }
//        BackstackImpl.Slider -> {
//          BackstackSlider(
//              screenList = screens,
//              scrollPosition = scrollPosition.value
//          ) {
//            AppScreen(name = it)
//          }
//        }
//      }
//    }
//
//    Slider(position = scrollPosition)
//  }
//}

@Preview
@Composable fun AppPreview() {
  App()
}
