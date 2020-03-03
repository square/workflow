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
package com.squareup.tracing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import okio.BufferedSink
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Encodes and writes [trace events][TraceEvent] to an Okio [BufferedSink].
 *
 * @param scope The [CoroutineScope] that defines the lifetime for the encoder. When the scope is
 * cancelled or fails, the sink returned from [sinkProvider] will be closed.
 * @param start The [TimeMark] to consider the beginning timestamp of the trace. All trace events'
 * timestamps are relative to this mark.
 * [TimeSource.Monotonic].[markNow][TimeSource.Monotonic.markNow] by default.
 * @param ioDispatcher The [CoroutineDispatcher] to use to execute all IO operations.
 * [IO] by default.
 * @param sinkProvider Returns the [BufferedSink] to use to write trace events to. Called on a
 * background thread.
 */
@OptIn(ExperimentalTime::class)
class TraceEncoder(
  scope: CoroutineScope,
  private val start: TimeMark = TimeSource.Monotonic.markNow(),
  ioDispatcher: CoroutineDispatcher = IO,
  private val sinkProvider: () -> BufferedSink
) : Closeable {

  private val processIdCounter = AtomicInteger(0)
  private val threadIdCounter = AtomicInteger(0)

  @Suppress("EXPERIMENTAL_API_USAGE")
  private val events: SendChannel<List<ChromeTraceEvent>> =
    scope.actor(ioDispatcher, capacity = UNLIMITED) {
      sinkProvider().use { sink ->
        // Start the JSON array. Doesn't need to be closed.
        sink.writeUtf8("[\n")

        @Suppress("EXPERIMENTAL_API_USAGE")
        consumeEach { eventBatch ->
          eventBatch.forEach { event ->
            event.writeTo(sink)
            sink.writeUtf8(",\n")
          }
          sink.flush()
        }
      }
    }

  /**
   * Allocates a new thread ID named [threadName] and returns a [TraceLogger] that will log all
   * events under that thread ID.
   *
   * Note this does not do anything with _actual_ threads, it just affects the thread ID used in
   * trace events.
   */
  fun createLogger(
    processName: String = "",
    threadName: String = ""
  ): TraceLogger {
    val processId = processIdCounter.getAndIncrement()
    val threadId = threadIdCounter.getAndIncrement()

    // Log metadata to set thread and process names.
    val timestamp = getTimestampNow()
    val processNameEvent = createProcessNameEvent(processName, processId, timestamp)
    val threadNameEvent = createThreadNameEvent(threadName, processId, threadId, timestamp)
    events.safeOffer(listOf(processNameEvent, threadNameEvent))

    return object : TraceLogger {
      override fun log(eventBatch: List<TraceEvent>) = log(processId, threadId, eventBatch)
      override fun log(event: TraceEvent) = log(processId, threadId, event)
      override fun toString(): String =
        " TraceLogger(" +
            "processName=$processName, processId=$processId, " +
            "threadName=$threadName, threadId=$threadId)"
    }
  }

  override fun close() {
    events.close()
  }

  internal fun log(
    processId: Int,
    threadId: Int,
    eventBatch: List<TraceEvent>
  ) {
    val timestampMicros = getTimestampNow()
    val chromeTraceEvents = eventBatch.map {
      it.toChromeTraceEvent(threadId, processId, timestampMicros)
    }
    events.safeOffer(chromeTraceEvents)
  }

  internal fun log(
    processId: Int,
    threadId: Int,
    event: TraceEvent
  ) {
    val timestampMicros = getTimestampNow()
    val chromeTraceEvents = event.toChromeTraceEvent(threadId, processId, timestampMicros)
    events.safeOffer(listOf(chromeTraceEvents))
  }

  private fun getTimestampNow(): Long = start.elapsedNow()
      .inMicroseconds
      .toLong()

  /**
   * Like [SendChannel.offer] but won't throw if the channel is closed.
   */
  private fun <T> SendChannel<T>.safeOffer(value: T) {
    try {
      offer(value)
    } catch (e: CancellationException) {
      // Ignore it.
    } catch (e: ClosedSendChannelException) {
      // Ignore it.
    }
  }
}
