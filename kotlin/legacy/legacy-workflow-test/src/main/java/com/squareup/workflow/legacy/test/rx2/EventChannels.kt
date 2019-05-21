/*
 * Copyright 2018 Square Inc.
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
@file:JvmName("EventChannels")
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.test.rx2

import com.squareup.workflow.legacy.rx2.EventChannel
import com.squareup.workflow.legacy.rx2.asEventChannel
import kotlinx.coroutines.channels.Channel

/**
 * Creates an [EventChannel] that will send all the values passed, and then throw if another
 * select is attempted.
 */
fun <E : Any> eventChannelOf(vararg values: E): EventChannel<E> =
  Channel<E>(values.size)
      .apply {
        // Load all the values into the channel's buffer.
        values.forEach { check(offer(it)) }
        // Ensure any receives after the values are read will fail.
        close()
      }
      .asEventChannel()
