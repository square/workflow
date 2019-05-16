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
package com.squareup.workflow.ui

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.os.Parcelable
import android.support.annotation.CheckResult
import android.support.v4.app.FragmentActivity
import com.squareup.workflow.Workflow
import io.reactivex.Flowable
import io.reactivex.Observable

/**
 * Packages a [Workflow] and a [ViewRegistry] to drive an [Activity][FragmentActivity].
 *
 * You'll never instantiate one of these yourself. Instead, use
 * [FragmentActivity.setContentWorkflow]. See that method for more details.
 */
@ExperimentalWorkflowUi
class WorkflowActivityRunner<out OutputT : Any, out RenderingT : Any>
internal constructor(private val model: WorkflowViewModel<OutputT, RenderingT>) {

  internal val renderings: Observable<out RenderingT> = model.updates.map { it.rendering }

  val viewRegistry: ViewRegistry = model.viewRegistry

  /**
   * A stream of the [output][OutputT] values emitted by the [Workflow]
   * managed by this model.
   */
  val output: Observable<out OutputT> = model.updates.filter { it.output != null }
      .map { it.output!! }

  /**
   * Returns a [Parcelable] instance of [PickledWorkflow] to be written
   * to the bundle passed to [FragmentActivity.onSaveInstanceState].
   * Read it back out in [FragmentActivity.onCreate], to serve as the
   * final argument to [FragmentActivity.setContentWorkflow].
   */
  fun asParcelable(): Parcelable = PickledWorkflow(model.lastSnapshot)

  /**
   * To be called from [FragmentActivity.onBackPressed], to give the managed
   * [Workflow] access to back button events.
   *
   * e.g.:
   *
   *    override fun onBackPressed() {
   *      if (!workflowViewModel.onBackPressed(this)) super.onBackPressed()
   *    }
   */
  fun onBackPressed(activity: Activity): Boolean {
    return HandlesBack.Helper.onBackPressed(activity.findViewById(R.id.workflow_activity_layout))
  }
}

/**
 * Call this method from [FragmentActivity.onCreate], instead of [FragmentActivity.setContentView].
 * It creates a [WorkflowActivityRunner] for this activity, if one doesn't already exist, and
 * sets a view driven by that model as the content view.
 *
 * Hold onto the [WorkflowActivityRunner] returned and:
 *
 *  - Call [WorkflowActivityRunner.onBackPressed] from [FragmentActivity.onBackPressed] to allow
 *    workflows to handle back button events. (See [HandlesBack] for more details.)
 *
 *  - Write [WorkflowActivityRunner.asParcelable] to the bundle passed to
 *    [FragmentActivity.onSaveInstanceState]. You'll read that [parcelable][PickledWorkflow]
 *    back in [FragmentActivity.onCreate], for use by the next call to this method.
 *
 *  e.g.:
 *
 *     class MainActivity : AppCompatActivity() {
 *       private lateinit var runner: WorkflowRunner<*, *>
 *
 *       override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         val restored = savedInstanceState?.getParcelable<PickledWorkflow>("WORKFLOW")
 *         runner = setContentWorkflow(MyViewRegistry, MyRootWorkflow(), restored)
 *       }
 *
 *       override fun onBackPressed() {
 *         if (!runner.onBackPressed(this)) super.onBackPressed()
 *       }
 *
 *       override fun onSaveInstanceState(outState: Bundle) {
 *         super.onSaveInstanceState(outState)
 *         outState.putParcelable("WORKFLOW", runner.asParcelable)
 *       }
 *     }
 */
@ExperimentalWorkflowUi
@CheckResult
fun <InputT, OutputT : Any, RenderingT : Any> FragmentActivity.setContentWorkflow(
  viewRegistry: ViewRegistry,
  workflow: Workflow<InputT, OutputT, RenderingT>,
  inputs: Flowable<InputT>,
  restored: PickledWorkflow?
): WorkflowActivityRunner<OutputT, RenderingT> {
  val factory = WorkflowViewModel.Factory(viewRegistry, workflow, inputs, restored)

  // We use an Android lifecycle ViewModel to shield ourselves from configuration changes.
  // ViewModelProviders.of() uses the factory to instantiate a new instance only
  // on the first call for this activity, and it stores that instance for repeated use
  // until this activity is finished.

  @Suppress("UNCHECKED_CAST")
  val viewModel = ViewModelProviders.of(this, factory)[WorkflowViewModel::class.java]
      as WorkflowViewModel<OutputT, RenderingT>
  val runner = WorkflowActivityRunner(viewModel)

  val layout = WorkflowLayout(this@setContentWorkflow)
      .apply {
        id = R.id.workflow_activity_layout
        setWorkflowRunner(runner)
      }

  this.setContentView(layout)

  return runner
}

/**
 * Convenience overload of [setContentWorkflow] for workflows that take no input.
 */
@ExperimentalWorkflowUi
@CheckResult
fun <OutputT : Any, RenderingT : Any> FragmentActivity.setContentWorkflow(
  viewRegistry: ViewRegistry,
  workflow: Workflow<Unit, OutputT, RenderingT>,
  restored: PickledWorkflow?
): WorkflowActivityRunner<OutputT, RenderingT> {
  return setContentWorkflow(viewRegistry, workflow, Flowable.fromArray(Unit), restored)
}
