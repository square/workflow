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
package com.squareup.workflow.util

/**
 * Provides a more definitive signal between "[value emitted][Value]" and "[channel closed][Closed]"
 * for channels that may contain nullable values.
 */
sealed class ChannelUpdate<out E> {

  /**
   * Indicates a [value] was received from the channel.
   *
   * @param value The value received from the channel.
   */
  data class Value<out E>(val value: E) : ChannelUpdate<E>()

  /**
   * Indicates that the channel was closed, and will never emit any more values.
   */
  object Closed : ChannelUpdate<Nothing>()
}
