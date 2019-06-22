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
@file:Suppress("LongParameterList")

package com.squareup.workflow.ui

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.squareup.workflow.Workflow
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.reactive.openSubscription

/**
 * Uses a [Workflow] and a [ViewRegistry] to drive a [WorkflowLayout].
 *
 * It is simplest to use
 * [Activity.setContentWorkflow][android.support.v4.app.FragmentActivity.setContentWorkflow]
 * or subclass [WorkflowFragment] rather than instantiate a [WorkflowRunner] directly.
 */
@ExperimentalWorkflowUi
interface WorkflowRunner<out OutputT> {
  /**
   * To be called from [FragmentActivity.onSaveInstanceState] or [Fragment.onSaveInstanceState].
   */
  fun onSaveInstanceState(outState: Bundle)

  /**
   * A stream of the [output][OutputT] values emitted by the running
   * [Workflow][com.squareup.workflow.Workflow].
   */
  val output: Flowable<out OutputT>

  val renderings: Observable<out Any>

  val viewRegistry: ViewRegistry

  companion object {
    /**
     * Returns a [ViewModel][android.arch.lifecycle.ViewModel] implementation of
     * [WorkflowRunner], tied to the given [activity].
     *
     * It's probably more convenient to use [FragmentActivity.setContentWorkflow]
     * rather than calling this method directly.
     *
     * @param inputs Function that returns a channel that delivers input values for the root
     * workflow. The first value emitted is passed to `initialState` to determine the root
     * workflow's initial state, and subsequent emissions are passed as input updates to the root
     * workflow. The channel returned by this function will be cancelled by the host when it's
     * finished.
     */
    private fun <InputT, OutputT : Any> of(
      activity: FragmentActivity,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      inputs: () -> ReceiveChannel<InputT>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> {
      val factory = WorkflowRunnerViewModel.Factory(
          workflow, viewRegistry, inputs, savedInstanceState, dispatcher
      )
      @Suppress("UNCHECKED_CAST")
      return ViewModelProviders.of(activity, factory)[WorkflowRunnerViewModel::class.java]
          as WorkflowRunner<OutputT>
    }

    /**
     * Returns a [ViewModel][android.arch.lifecycle.ViewModel] implementation of
     * [WorkflowRunner], tied to the given [activity].
     *
     * It's probably more convenient to use [FragmentActivity.setContentWorkflow]
     * rather than calling this method directly.
     */
    @UseExperimental(ObsoleteCoroutinesApi::class)
    fun <InputT, OutputT : Any> of(
      activity: FragmentActivity,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      inputs: Flowable<InputT>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> = of<InputT, OutputT>(
        activity, viewRegistry, workflow, { inputs.openSubscription() }, savedInstanceState,
        dispatcher
    )

    /**
     * Convenience overload for workflows unconcerned with back-pressure of their inputs.
     */
    fun <InputT, OutputT : Any> of(
      activity: FragmentActivity,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      inputs: Observable<InputT>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> = of(
        activity, viewRegistry, workflow, inputs.toFlowable(LATEST), savedInstanceState, dispatcher
    )

    /**
     * Convenience overload for workflows that take one input value rather than a stream.
     */
    fun <InputT, OutputT : Any> of(
      activity: FragmentActivity,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      input: InputT,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> = of(
        activity, viewRegistry, workflow, Observable.just(input), savedInstanceState, dispatcher
    )

    /**
     * Convenience overload for workflows that take no input.
     */
    fun <OutputT : Any> of(
      activity: FragmentActivity,
      viewRegistry: ViewRegistry,
      workflow: Workflow<Unit, OutputT, Any>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> {
      return of(activity, viewRegistry, workflow, Unit, savedInstanceState, dispatcher)
    }

    /**
     * Returns a [ViewModel][android.arch.lifecycle.ViewModel] implementation of
     * [WorkflowRunner], tied to the given [fragment].
     *
     * It's probably more convenient to subclass [WorkflowFragment] rather than calling
     * this method directly.
     *
     * @param inputs Function that returns a channel that delivers input values for the root
     * workflow. The first value emitted is passed to `initialState` to determine the root
     * workflow's initial state, and subsequent emissions are passed as input updates to the root
     * workflow. The channel returned by this function will be cancelled by the host when it's
     * finished.
     */
    private fun <InputT, OutputT : Any> of(
      fragment: Fragment,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      inputs: () -> ReceiveChannel<InputT>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> {
      val factory =
        WorkflowRunnerViewModel.Factory(
            workflow, viewRegistry, inputs, savedInstanceState, dispatcher
        )
      @Suppress("UNCHECKED_CAST")
      return ViewModelProviders.of(fragment, factory)[WorkflowRunnerViewModel::class.java]
          as WorkflowRunner<OutputT>
    }

    /**
     * Returns a [ViewModel][android.arch.lifecycle.ViewModel] implementation of
     * [WorkflowRunner], tied to the given [fragment].
     *
     * It's probably more convenient to subclass [WorkflowFragment] rather than calling
     * this method directly.
     */
    @UseExperimental(ObsoleteCoroutinesApi::class)
    fun <InputT, OutputT : Any> of(
      fragment: Fragment,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      inputs: Flowable<InputT>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> = of<InputT, OutputT>(
        fragment, viewRegistry, workflow, { inputs.openSubscription() }, savedInstanceState,
        dispatcher
    )

    /**
     * Convenience overload for workflows unconcerned with back-pressure of their inputs.
     */
    fun <InputT, OutputT : Any> of(
      fragment: Fragment,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      inputs: Observable<InputT>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> = of(
        fragment, viewRegistry, workflow, inputs.toFlowable(LATEST), savedInstanceState,
        dispatcher
    )

    /**
     * Convenience overload for workflows that take one input value rather than a stream.
     */
    fun <InputT, OutputT : Any> of(
      fragment: Fragment,
      viewRegistry: ViewRegistry,
      workflow: Workflow<InputT, OutputT, Any>,
      input: InputT,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> =
      of(fragment, viewRegistry, workflow, Flowable.just(input), savedInstanceState, dispatcher)

    /**
     * Convenience overload for workflows that take no input.
     */
    fun <OutputT : Any> of(
      fragment: Fragment,
      viewRegistry: ViewRegistry,
      workflow: Workflow<Unit, OutputT, Any>,
      savedInstanceState: Bundle?,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): WorkflowRunner<OutputT> {
      return of(fragment, viewRegistry, workflow, Unit, savedInstanceState, dispatcher)
    }
  }
}

/**
 * Call this method from [FragmentActivity.onCreate], instead of [FragmentActivity.setContentView].
 * It creates a [WorkflowRunner] for this activity, if one doesn't already exist, and
 * sets a view driven by that model as the content view.
 *
 * Hold onto the [WorkflowRunner] returned and:
 *
 *  - Call [FragmentActivity.workflowOnBackPressed] from [FragmentActivity.onBackPressed] to allow
 *    workflows to handle back button events. (See [HandlesBack] for more details.)
 *
 *  - Call [WorkflowRunner.onSaveInstanceState] from [FragmentActivity.onSaveInstanceState].
 *
 *  e.g.:
 *
 *     class MainActivity : AppCompatActivity() {
 *       private lateinit var runner: WorkflowRunner<*, *>
 *
 *       override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         runner = setContentWorkflow(MyViewRegistry, MyRootWorkflow(), savedInstanceState)
 *       }
 *
 *       override fun onBackPressed() {
 *         if (!runner.onBackPressed(this)) super.onBackPressed()
 *       }
 *
 *       override fun onSaveInstanceState(outState: Bundle) {
 *         super.onSaveInstanceState(outState)
 *         runner.onSaveInstanceState(outState)
 *       }
 *     }
 */
@ExperimentalWorkflowUi
@CheckResult
fun <InputT, OutputT : Any> FragmentActivity.setContentWorkflow(
  viewRegistry: ViewRegistry,
  workflow: Workflow<InputT, OutputT, Any>,
  inputs: Flowable<InputT>,
  savedInstanceState: Bundle?,
  dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): WorkflowRunner<OutputT> {
  val runner = WorkflowRunner.of(
      this, viewRegistry, workflow, inputs, savedInstanceState, dispatcher
  )
  val layout = WorkflowLayout(this@setContentWorkflow)
      .apply {
        id = R.id.workflow_layout
        start(runner)
      }

  this.setContentView(layout)

  return runner
}

/**
 * If your workflow needs to manage the back button, override [FragmentActivity.onBackPressed]
 * and call this method, and have its views or [LayoutRunner]s use [HandlesBack].
 *
 * e.g.:
 *
 *    override fun onBackPressed() {
 *      if (!workflowOnBackPressed()) super.onBackPressed()
 *    }
 *
 * **Only for use by activities driven via [FragmentActivity.setContentWorkflow].**
 *
 * @see WorkflowFragment.onBackPressed
 */
@ExperimentalWorkflowUi
@CheckResult
fun FragmentActivity.workflowOnBackPressed(): Boolean {
  return HandlesBack.Helper.onBackPressed(this.findViewById(R.id.workflow_layout))
}

/**
 * Convenience overload for workflows unconcerned with back-pressure of their inputs.
 */
@ExperimentalWorkflowUi
@CheckResult
fun <InputT, OutputT : Any, RenderingT : Any> FragmentActivity.setContentWorkflow(
  viewRegistry: ViewRegistry,
  workflow: Workflow<InputT, OutputT, RenderingT>,
  inputs: Observable<InputT>,
  savedInstanceState: Bundle?,
  dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): WorkflowRunner<OutputT> = setContentWorkflow(
    viewRegistry, workflow, inputs.toFlowable(LATEST), savedInstanceState, dispatcher
)

/**
 * Convenience overload for workflows that take one input value rather than a stream.
 */
@ExperimentalWorkflowUi
@CheckResult
fun <InputT, OutputT : Any, RenderingT : Any> FragmentActivity.setContentWorkflow(
  viewRegistry: ViewRegistry,
  workflow: Workflow<InputT, OutputT, RenderingT>,
  input: InputT,
  savedInstanceState: Bundle?,
  dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): WorkflowRunner<OutputT> =
  setContentWorkflow(viewRegistry, workflow, Flowable.just(input), savedInstanceState, dispatcher)

/**
 * Convenience overload for workflows that take no input.
 */
@ExperimentalWorkflowUi
@CheckResult
fun <OutputT : Any, RenderingT : Any> FragmentActivity.setContentWorkflow(
  viewRegistry: ViewRegistry,
  workflow: Workflow<Unit, OutputT, RenderingT>,
  savedInstanceState: Bundle?,
  dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): WorkflowRunner<OutputT> =
  setContentWorkflow(viewRegistry, workflow, Unit, savedInstanceState, dispatcher)
