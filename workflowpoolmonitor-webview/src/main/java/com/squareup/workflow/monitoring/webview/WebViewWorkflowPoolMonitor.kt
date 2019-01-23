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
package com.squareup.workflow.monitoring.webview

import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPoolMonitor
import com.squareup.workflow.WorkflowPoolMonitorEvent
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitor
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.header
import io.ktor.response.respondOutputStream
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import okio.Okio.sink
import org.slf4j.event.Level.INFO
import java.util.concurrent.TimeUnit.MILLISECONDS

private val CONTENT_TYPE_EVENT_STREAM = ContentType.parse("text/event-stream")

/**
 * @param port The port to listen on.
 * @param host The IP address of the network interface to bind to. Defaults to localhost for
 * privacy, since monitor events may contain sensitive information.
 * @param microsecondClock The clock used to trace workflow events.
 * See [TracingWorkflowPoolMonitor].
 */
class WebViewWorkflowPoolMonitor(
  val port: Int,
  host: String = "127.0.0.1",
  microsecondClock: () -> Long = TracingWorkflowPoolMonitor.systemMicrosecondClock
) : WorkflowPoolMonitor {

  private val tracingMonitor = TracingWorkflowPoolMonitor(microsecondClock)
  private val eventChannel = BroadcastChannel<WorkflowPoolMonitorEvent>(capacity = 5000)

  private val server = embeddedServer(Jetty, port, host) {

    install(CallLogging) {
      level = INFO
    }

    routing {
      static("/") {
        resources("/static_web")
        defaultResource("index.html", "/static_web")
      }

      get("/state_updates") {
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.respondTextWriter(contentType = CONTENT_TYPE_EVENT_STREAM) {
          eventChannel.openSubscription()
              .consumeEach { event ->
                appendln("data: $event")
                appendln()
                flush()
              }
        }
      }

      get("/workflow_trace.json") {
        call.response.header(
            HttpHeaders.ContentDisposition, """attachment; filename="workflow_trace.json""""
        )
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.respondOutputStream(contentType = ContentType.Application.Json) {
          tracingMonitor.writeTraceFile(sink(this))
        }
      }
    }
  }

  fun start(wait: Boolean = false) {
    server.start(wait)
  }

  fun stop() {
    server.stop(
        gracePeriod = 0,
        timeout = 0,
        timeUnit = MILLISECONDS
    )
  }

  override fun report(
    pool: WorkflowPool,
    event: WorkflowPoolMonitorEvent
  ) {
    tracingMonitor.report(pool, event)
    eventChannel.offer(event)
  }
}
