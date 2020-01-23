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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.toLiveData
import com.squareup.workflow.Workflow
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.ui.WorkflowRunner.Config
import io.reactivex.Maybe
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Uses a [Workflow] and a [ViewRegistry] to drive a [WorkflowLayout].
 *
 * It is simplest to use [Activity.setContentWorkflow][setContentWorkflow]
 * or subclass [WorkflowFragment] rather than instantiate a [WorkflowRunner] directly.
 */
interface WorkflowRunner<out OutputT : Any> {
  /**
   * Provides the first (and only) [OutputT] value emitted by the workflow, or
   * nothing if it is canceled before emitting.
   *
   * The output of the root workflow is treated as a result code, handy for use
   * as a sign that the host Activity or Fragment should be finished. Thus, once
   * a value is emitted the workflow is ended its output value is reported through
   * this field.
   */
  val result: Maybe<out OutputT>

  /**
   * A stream of the rendering values emitted by the running [Workflow].
   */
  val renderings: Observable<out Any>

  /**
   * @param diagnosticListener If non-null, will receive all diagnostic events from the workflow
   * runtime. See [com.squareup.workflow.WorkflowSession.diagnosticListener].
   */
  class Config<PropsT, OutputT : Any>(
    val workflow: Workflow<PropsT, OutputT, Any>,
    val props: Flow<PropsT>,
    val dispatcher: CoroutineDispatcher,
    val diagnosticListener: WorkflowDiagnosticListener?
  ) {
    /**
     * @param diagnosticListener If non-null, will receive all diagnostic events from the workflow
     * runtime. See [com.squareup.workflow.WorkflowSession.diagnosticListener].
     */
    constructor(
      workflow: Workflow<PropsT, OutputT, Any>,
      props: PropsT,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
      diagnosticListener: WorkflowDiagnosticListener? = null
    ) : this(workflow, flowOf(props), dispatcher, diagnosticListener)
  }

  companion object {
    /**
     * @param diagnosticListener If non-null, will receive all diagnostic events from the workflow
     * runtime. See [com.squareup.workflow.WorkflowSession.diagnosticListener].
     */
    @Suppress("FunctionName")
    fun <OutputT : Any> Config(
      workflow: Workflow<Unit, OutputT, Any>,
      dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
      diagnosticListener: WorkflowDiagnosticListener? = null
    ): Config<Unit, OutputT> {
      return Config(workflow, Unit, dispatcher, diagnosticListener)
    }

    /**
     * Returns an instance of [WorkflowRunner] tied to the
     * [Lifecycle][androidx.lifecycle.Lifecycle] of the given [activity].
     *
     * It's probably more convenient to use [FragmentActivity.setContentWorkflow]
     * rather than calling this method directly.
     *
     * @param configure function defining the root workflow and its environment. Called only
     * once per [lifecycle][FragmentActivity.getLifecycle], and always called from the UI thread.
     */
    fun <PropsT, OutputT : Any> startWorkflow(
      activity: FragmentActivity,
      configure: () -> Config<PropsT, OutputT>
    ): WorkflowRunner<OutputT> {
      val factory = WorkflowRunnerViewModel.Factory(activity.savedStateRegistry, configure)

      @Suppress("UNCHECKED_CAST")
      return ViewModelProvider(activity, factory)[WorkflowRunnerViewModel::class.java]
          as WorkflowRunner<OutputT>
    }

    /**
     * Returns an instance of [WorkflowRunner] tied to the
     * [Lifecycle][androidx.lifecycle.Lifecycle] of the given [fragment].
     *
     * It's probably more convenient to subclass [WorkflowFragment] rather than calling
     * this method directly.
     *
     * @param configure function defining the root workflow and its environment. Called only
     * once per [lifecycle][Fragment.getLifecycle], and always called from the UI thread.
     */
    fun <PropsT, OutputT : Any> startWorkflow(
      fragment: Fragment,
      configure: () -> Config<PropsT, OutputT>
    ): WorkflowRunner<OutputT> {
      val factory = WorkflowRunnerViewModel.Factory(fragment.savedStateRegistry, configure)

      @Suppress("UNCHECKED_CAST")
      return ViewModelProvider(fragment, factory)[WorkflowRunnerViewModel::class.java]
          as WorkflowRunner<OutputT>
    }
  }
}

/**
 * Call this method from [FragmentActivity.onCreate], instead of [FragmentActivity.setContentView].
 * It creates a [WorkflowRunner] for this activity, if one doesn't already exist, and
 * sets a view driven by that model as the content view.
 *
 * @param containerHints provides the [ViewRegistry] used to display workflow renderings.
 *
 * @param configure function defining the root workflow and its environment. Called only
 * once per [lifecycle][FragmentActivity.getLifecycle], and always called from the UI thread.
 *
 * @param onResult function called with the first (and only) output emitted by the root workflow,
 * handy for passing to [FragmentActivity.setResult]. The workflow is ended once it emits any
 * values, so this is also a good place from which to call [FragmentActivity.finish]. Called
 * only while the activity is active, and always called from the UI thread.
 */
fun <PropsT, OutputT : Any> FragmentActivity.setContentWorkflow(
  containerHints: ContainerHints,
  configure: () -> Config<PropsT, OutputT>,
  onResult: (OutputT) -> Unit
): WorkflowRunner<OutputT> {
  val runner = WorkflowRunner.startWorkflow(this, configure)
  val layout = WorkflowLayout(this@setContentWorkflow).apply {
    id = R.id.workflow_layout
    start(runner.renderings, containerHints)
  }

  runner.result.toFlowable()
      .toLiveData()
      .observe(this, Observer { result -> onResult(result) })

  this.setContentView(layout)

  return runner
}

/**
 * Call this method from [FragmentActivity.onCreate], instead of [FragmentActivity.setContentView].
 * It creates a [WorkflowRunner] for this activity, if one doesn't already exist, and
 * sets a view driven by that model as the content view.
 *
 * @param registry used to display workflow renderings.
 *
 * @param configure function defining the root workflow and its environment. Called only
 * once per [lifecycle][FragmentActivity.getLifecycle], and always called from the UI thread.
 *
 * @param onResult function called with the first (and only) output emitted by the root workflow,
 * handy for passing to [FragmentActivity.setResult]. The workflow is ended once it emits any
 * values, so this is also a good place from which to call [FragmentActivity.finish]. Called
 * only while the activity is active, and always called from the UI thread.
 */
fun <PropsT, OutputT : Any> FragmentActivity.setContentWorkflow(
  registry: ViewRegistry,
  configure: () -> Config<PropsT, OutputT>,
  onResult: (OutputT) -> Unit
): WorkflowRunner<OutputT> = setContentWorkflow(ContainerHints(registry), configure, onResult)

/**
 * For workflows that produce no output, call this method from [FragmentActivity.onCreate]
 * instead of [FragmentActivity.setContentView].
 * It creates a [WorkflowRunner] for this activity, if one doesn't already exist, and
 * sets a view driven by that model as the content view.
 *
 * @param containerHints provides the [ViewRegistry] used to display workflow renderings.
 *
 * @param configure function defining the root workflow and its environment. Called only
 * once per [lifecycle][FragmentActivity.getLifecycle], and always called from the UI thread.
 */
fun <PropsT> FragmentActivity.setContentWorkflow(
  containerHints: ContainerHints,
  configure: () -> Config<PropsT, Nothing>
): WorkflowRunner<Nothing> = setContentWorkflow(containerHints, configure) {}

/**
 * For workflows that produce no output, call this method from [FragmentActivity.onCreate]
 * instead of [FragmentActivity.setContentView].
 * It creates a [WorkflowRunner] for this activity, if one doesn't already exist, and
 * sets a view driven by that model as the content view.
 *
 * @param registry used to display workflow renderings.
 *
 * @param configure function defining the root workflow and its environment. Called only
 * once per [lifecycle][FragmentActivity.getLifecycle], and always called from the UI thread.
 */
fun <PropsT> FragmentActivity.setContentWorkflow(
  registry: ViewRegistry,
  configure: () -> Config<PropsT, Nothing>
): WorkflowRunner<Nothing> = setContentWorkflow(ContainerHints(registry), configure) {}
