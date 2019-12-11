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
package com.squareup.workflow

import com.squareup.workflow.FractalWorkflow.Props
import com.squareup.workflow.FractalWorkflow.Props.DO_NOT_RENDER_LEAVES
import com.squareup.workflow.FractalWorkflow.Props.RENDER_LEAVES
import com.squareup.workflow.FractalWorkflow.Props.RUN_WORKERS
import com.squareup.workflow.FractalWorkflow.Props.SKIP_FIRST_LEAF
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.internal.WorkflowNode
import com.squareup.workflow.internal.id
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode.AverageTime
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit.MICROSECONDS
import java.util.concurrent.TimeUnit.SECONDS

@Fork(value = 1)
@State(Scope.Thread)
@BenchmarkMode(AverageTime)
@Warmup(iterations = 3, time = 5, timeUnit = SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = SECONDS)
@OutputTimeUnit(MICROSECONDS)
open class WorkflowNodeBenchmark {

  /**
   * Used to parameterize the benchmarks.
   *
   * @param childCount The number of children _each workflow in the tree_ should have.
   * @param depth The depth of the workflow tree.
   */
  enum class TreeShape(
    val childCount: Int,
    val depth: Int
  ) {
    // A tree with childCount = 4 and depth = 4 has 341 nodes, so keep the node count consistent
    // across all benchmarks.
    DEEP(childCount = 1, depth = 340),
    BUSHY(childCount = 340, depth = 1),
    SQUARE(childCount = 4, depth = 4);

    override fun toString(): String = "$name(childCount=$childCount, depth=$depth)"
  }

  private val context = Unconfined

  @Param
  @JvmField var treeShape = TreeShape.SQUARE

  private lateinit var workflow: FractalWorkflow
  private lateinit var node: WorkflowNode<Props, Unit, Nothing, Unit>

  @Setup open fun setUp() {
    println("Tree shape: $treeShape")
    workflow = FractalWorkflow(treeShape.childCount, treeShape.depth)
    node = workflow.createNode()
  }

  /**
   * Always renders leaves. After the first render pass, the tree structure never changes, so this
   * measures no-op render passes.
   */
  @Benchmark open fun renderPassAlwaysLeaves() {
    node.render(workflow, RENDER_LEAVES)
    // Second render to be consistent with other benchmarks that need two render passes.
    node.render(workflow, RENDER_LEAVES)
  }

  /**
   * Alternates between rendering leaves and not rendering leaves. The tree structure is always
   * changing, as well as the props all the way down the tree, so this prevents props caching
   * and measures setup/teardown cost for [WorkflowNode]s.
   */
  @Benchmark open fun renderPassAlternateLeaves() {
    node.render(workflow, RENDER_LEAVES)
    node.render(workflow, DO_NOT_RENDER_LEAVES)
  }

  /**
   * Alternates between having every leaf run a worker, and having no leaves run workers.
   * Measures setup/teardown cost for [Worker]s.
   */
  @Benchmark open fun renderPassAlternateWorkers() {
    node.render(workflow, RENDER_LEAVES)
    node.render(workflow, RUN_WORKERS)
  }

  /**
   * This benchmark is equivalent to [renderPassAlternateLeaves] for the [TreeShape.DEEP] case.
   */
  @Benchmark open fun renderPassAlternateOneLeaf() {
    node.render(workflow, RENDER_LEAVES)
    node.render(workflow, SKIP_FIRST_LEAF)
  }

  private fun FractalWorkflow.createNode() = WorkflowNode(
      id = this.id(),
      workflow = this,
      initialProps = RENDER_LEAVES,
      snapshot = null,
      baseContext = context
  )
}

/**
 * A stateless, outputless, rendering-less workflow that will recursively form a tree.
 *
 * Subclasses [StatefulWorkflow], not `StatelessWorkflow`, to reduce the number of allcations
 * because `StatelessWorkflow` allocates a `StatefulWorkflow` under the hood.
 */
private class FractalWorkflow(
  private val childCount: Int,
  private val depth: Int
) : StatefulWorkflow<Props, Unit, Nothing, Unit>() {

  enum class Props(
    val renderLeaves: Boolean,
    val runWorkers: Boolean = false,
    val skipFirstLeaf: Boolean = false
  ) {
    RENDER_LEAVES(renderLeaves = true),
    DO_NOT_RENDER_LEAVES(renderLeaves = false),
    RUN_WORKERS(renderLeaves = false, runWorkers = true),
    SKIP_FIRST_LEAF(renderLeaves = true, skipFirstLeaf = true),
  }

  private val childWorkflow =
    if (depth > 0 && childCount > 0) FractalWorkflow(childCount, depth - 1)
    else null

  private val areChildrenLeaves = depth == 1

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ) = Unit

  override fun render(
    props: Props,
    state: Unit,
    context: RenderContext<Unit, Nothing>
  ) {
    if (childWorkflow != null && (props.renderLeaves || !areChildrenLeaves)) {
      for (i in 0 until childCount) {
        if (props.skipFirstLeaf) {
          // Don't render the first child if it's a leaf, otherwise render children using props that
          // will fractally result in the first leaf being skipped.
          if (!areChildrenLeaves || i > 0) {
            val childProps = if (i == 0) props else RENDER_LEAVES
            context.renderChild(childWorkflow, childProps, key = i.toString())
          }
        } else {
          context.renderChild(childWorkflow, props, key = i.toString())
        }
      }
    }

    if (props.runWorkers && depth == 0) {
      context.runningWorker(NeverWorker) { noAction() }
    }
  }

  override fun snapshotState(state: Unit): Snapshot = throw NotImplementedError()
}

private object NeverWorker : Worker<Unit> {
  override fun run(): Flow<Unit> = flow {
    suspendCancellableCoroutine<Nothing> {
      // Never emit, suspend indefinitely.
    }
  }
}
