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
package com.squareup.workflow.benchmarks

import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.launchWorkflowIn
import com.squareup.workflow.renderChild
import com.squareup.workflow.runningWorker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Mode.AverageTime
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS

@Fork(1)
@State(Scope.Benchmark)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(MILLISECONDS)
open class DeepRenderBenchmark {

  private val deepProps = CompletableDeferred<Int>()
  private lateinit var renderings: Flow<Int>
  private lateinit var dispatcher: ExecutorCoroutineDispatcher

  @UseExperimental(FlowPreview::class)
  @Setup open fun setUp() {
    dispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher()
    val runtimeScope = CoroutineScope(dispatcher)
    val deepWorkflow = DeepRenderWorkflow()

    launchWorkflowIn(runtimeScope, deepWorkflow, deepProps::await.asFlow()) { r, _ ->
      renderings = r.map { it.rendering.result }
    }
  }

  @TearDown open fun tearDown() {
    dispatcher.close()
  }

  @Benchmark open fun deepRender() {
    deepProps.complete(100)
    runBlocking {
      renderings.first()
    }
  }
}

private val EMPTY_WORKER = Worker.createSideEffect("empty") {}

/**
 * JMH pukes if this is nested inside the workflow.
 */
private data class DeepRendering(
  val result: Int,
  val dummyHandler: (Unit) -> Unit
)

private class DeepRenderWorkflow : StatelessWorkflow<Int, Nothing, DeepRendering>() {

  override fun render(
    input: Int,
    context: RenderContext<Nothing, Nothing>
  ): DeepRendering {
    context.runningWorker(EMPTY_WORKER)

    val child = if (input == 0) null else context.renderChild(this, input - 1)

    return DeepRendering(
        result = child?.result ?: 0,
        dummyHandler = context.onEvent { child?.dummyHandler; noAction() }
    )
  }
}
