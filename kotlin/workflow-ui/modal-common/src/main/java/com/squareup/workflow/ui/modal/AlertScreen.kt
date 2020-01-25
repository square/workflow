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
package com.squareup.workflow.ui.modal

/**
 * Models a typical "You sure about that?" alert box.
 */
data class AlertScreen(
  val buttons: Map<Button, String> = emptyMap(),
  val message: String = "",
  val title: String = "",
  val cancelable: Boolean = true,
  val onEvent: (Event) -> Unit
) {
  enum class Button {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
  }

  sealed class Event {
    data class ButtonClicked(val button: Button) : Event()

    object Canceled : Event()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AlertScreen

    return buttons == other.buttons &&
        message == other.message &&
        title == other.title &&
        cancelable == other.cancelable
  }

  override fun hashCode(): Int {
    var result = buttons.hashCode()
    result = 31 * result + message.hashCode()
    result = 31 * result + title.hashCode()
    result = 31 * result + cancelable.hashCode()
    return result
  }
}
