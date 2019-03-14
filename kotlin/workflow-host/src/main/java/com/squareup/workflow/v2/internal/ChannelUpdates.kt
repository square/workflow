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
package com.squareup.workflow.v2.internal

import com.squareup.workflow.v2.ChannelUpdate
import com.squareup.workflow.v2.ChannelUpdate.Closed
import com.squareup.workflow.v2.ChannelUpdate.Value
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.selects.SelectBuilder

/**
 * Like [ReceiveChannel.onReceiveOrNull], but uses [ChannelUpdate] to differentiate between closure
 * and null values.
 */
internal fun <E, R> SelectBuilder<R>.onChannelUpdate(
  channel: ReceiveChannel<E>,
  handler: (ChannelUpdate<E>) -> R
) {
  channel.onReceiveOrNull { value ->
    // Need to check closed flag on the channel in case the channel just emitted a legit null
    // value.
    val update = if (value == null && channel.isClosedForReceive) {
      Closed
    } else {
      // The cast is needed because E can be nullable, in which case E? == E, but the compiler
      // doesn't know that.
      @Suppress("UNCHECKED_CAST")
      (Value(value as E))
    }
    return@onReceiveOrNull handler(update)
  }
}
