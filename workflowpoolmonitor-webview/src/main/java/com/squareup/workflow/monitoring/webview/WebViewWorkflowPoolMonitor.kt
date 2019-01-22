package com.squareup.workflow.monitoring.webview

import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPoolMonitor
import com.squareup.workflow.WorkflowPoolMonitorEvent
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitor
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.response.header
import io.ktor.response.respondOutputStream
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.unsafe
import okio.Okio.sink
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
    routing {
      get("/") {
        call.respondHtml {
          head {
            title { +"Workflow Inspector" }
          }
          body {
            div { a(href = "/workflow_trace.json") { +"Download Trace File" } }

            p { +"Events:" }

            div {
              id = "result"
            }

            script {
              unsafe {
                +"""
                  var source = new EventSource("/state_updates");
                  source.onmessage = function(event) {
                    document.getElementById("result").innerHTML += event.data + "<br/>";
                  };
                  source.onerror = function(e) {
                    console.log("event source failed:", e);
                  };
                  source.onopen = function(e) {
                    console.log("event source opened: ", e);
                  };
                """.trimIndent()
              }
            }
          }
        }
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

  fun start() {
    server.start(wait = false)
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
    eventChannel.offer(event)
  }
}
