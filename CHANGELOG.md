Change Log
==========

## Version 0.28.0

_2020-05-12_

### Kotlin

* Breaking: Simplify `ViewRegistry` to act more like a simple map of rendering type to `ViewFactory`. (#1148)
* Extracted Compose support into [square/workflow-kotlin-compose](https://github.com/square/workflow-kotlin-compose). (#1147)
  * Artifacts for Compose support will be released separately from now on, which allows us to keep them
    updated more frequently as the Workflow library stabilizes.
* Remove RxJava2 dependency from Workflow Android UI integration. (#1150)
* Fix: Make `workflow-ui-android-core` declare `workflow-runtime` as `api` instead of `implementation` dependency. (#1144)
* Fix: Move `WorkflowFragment` initialization to `onViewStateRestored`. (#1133 – thanks @ychescale9!)
* Upgrade coroutines library to 1.3.6. (#1151)

### Swift

* Add os_signpost logs for `render()` and `Workflow` and`Worker` lifecycle. (#1134)

## Version 0.27.1

_2020-05-11_

### Kotlin

* Add insecure checksums gradle flag to fix Nexus release. (#1156)

### Swift

* Kotlin-only release, no changes.

## Version 0.27.0

_2020-05-04_

### Kotlin

* Update Compose to dev10. (#1118)

### Swift

* Remove context.subscribe (#1111)

## Version 0.26.0

_2020-04-17_

### Kotlin

* Upgrade kotlinx.serialization now that KAML supports it. (#1018)
* Makes BackStackContainer.update protected, required for customization. (#1088)

### Swift

* Vend `preferredContentSize` through the `DescribedViewController` (#1051)
* Add ContainerViewController init with AnyWorkflowConvertable (#1092)
* Provide empty `makeInitialState` and `workflowDidChange` when State is Void (#1094)

## Version 0.25.0

_2020-03-27_

### Kotlin

* Swift-only release. No changes.

### Swift

* Introduce `SignalWorker` to wrap `Signal`s (#1048)

## Version 0.24.0

_2020-03-10_

### Kotlin

 * Introduce a view binding function that works with AndroidX ViewBindings. (#985)
 * Rename ViewBinding to ViewFactory. (#1009)
 * Rename ContainerHints to ViewEnvironment to match Swift. (#1005)
 * Fix type parameter for expectWorker accepting Worker instance. (#969)

### Swift

 * Removes ViewRegistry, Introduce ViewControllerDescription. (#974)
 * Introduce ViewEnvironment. (#999)

## Version 0.23.3

### Kotlin

 * Fix type parameter for expectWorker accepting Worker instance. (#969)

### Swift

 * Kotlin-only release, no changes.

## Version 0.23.2

### Kotlin

 * Make the context used to start workers configurable for tests. (#940, #943, #950)

### Swift

 * Kotlin-only release, no changes.

## Version 0.23.1

### Kotlin

 * Swift-only release, no changes.

### Swift

 * Seperate `SwiftUI` support to a different module: `WorkflowSwiftUI`. (#929)

## Version 0.23.0

_2020-01-31_

### Kotlin

 * Make all workers run on the `Unconfined` dispatcher. (#851)
 * Paramaterize the return type of `Worker.finished()` to make it more convenient to use in tests. (#884)
 * Improved animation for `BackStackContainer`, `PanelContainer`. (#886)
 * Target JVM 1.8 bytecode for all modules. (#898)
 * Don't call `onPropsChanged` unless the old and new props are actually unequal. (#887)
 * Use KType instead of KClass in TypedWorker. (#908)
 * Make verifyAction and verifyActionResult support no processed action. (#909)
 * Drastically simplified ModalViewContainer. (#913)
 * Pass `acceptOutput` function to `WorkflowNode` constructor instead of every tick pass. (#916)
 * Break UI modules into: (#915)
   * `workflow-ui`
     * `core-common`
     * `core-android`
     * `modal-common`
     * `modal-android`
     * `backstack-common`
     * `backstack-android`

### Swift

 * No changes.

## Version 0.22.4

_2020-01-15_

### Kotlin

 * Allow empty snapshots to be passed into launchWorkflowIn (#881)

### Swift

 * Flag out WorkflowTesting APIs when in Release mode  (#872)

## Version 0.22.3

_2020-01-10_

### Kotlin

 * Adds `ContainerHints` to `ShowRenderingTag`. (#868)
 * Add an empty `ViewRegistry` factory function. (#870)

### Swift

 * Kotlin-only release, no changes.

## Version 0.22.2

_2020-01-07_

### Kotlin

 * Breaking: WorkflowAction.invoke is now action. (#862)
 * Fixes StatelessWorkflow.action. (#861)

### Swift

 * Kotlin-only release, no changes.

## Version 0.22.1

_2019-12-20_

### Kotlin

 * Make WorkflowAction covariant on OutputT again. (#837)
 * Undo making RenderContext a sink, give it a sink property instead. (#839)

### Swift

 * Kotlin-only release, no changes.

## Version 0.22.0

_2019-12-18_

### Kotlin

#### Breaking changes:

 * Safer `BackStackScreen` construction. (#809)
 * Pass `ContainerHints` via `setContentWorkflow`. (#808)
 * `ViewRegistry` is now a `ContainerHint`. (#770)
 * Remove deprecated `testRender` API. (#743)

#### Non-breaking API changes:

 * Make `lifecyclOrNull` public. (#747)
 * Give `Worker.doesSameWorkAs` a default implementation that just compares by concrete type. (#746)
 * Replace `WorkflowAction.Mutator` API with more ergonomic `.Updater` (#812, #813)
 * Give `RenderTester.render` function argument a default no-op value. (#828)
 * Add overload to `RenderTester.expectWorker` for simple worker comparisons. (#828)
 * Make `RenderContext` itself a Sink. (#835)
 * Convert `ViewRegistry` to an interface. (#832)

#### Other changes:

 * Add variance to StatelessWorkflow type parameters. (#790)
 * Make event sinks queued and reusable instead of throw after going stale. (#742)
 * Optimize re-rendering child workflows. (#800)

### Swift

 * Add macOS support (#750)
 * Add `ContainerView` to enable SwiftUI integration. (#691)

## Version 0.21.3

_2019-11-18_

### Kotlin

 * Fix RenderTester not allowing output-less expectations to be specified after one with output. (#756)
 * Fix RenderTester not checking worker keys. (#755)

### Swift

 * Kotlin-only release, no changes.

## Version 0.21.2

_2019-11-7_

### Kotlin

 * Restore `testFromState`. (#716)
 * New render testing API for kotlin. (#687)

### Swift

 * Kotlin-only release, no changes.

## Version 0.21.1

_2019-10-30_

### Kotlin

 * Publish `internal-testing-utils` artifact that is required by two of the other modules. (#708)
 * Add missing kdoc for `TraceLogger` (#705)

### Swift

 * Kotlin-only release, no changes.

## Version 0.21.0

_2019-10-29_

### Kotlin

#### Core API changes:

 * Add `StatefulWorkflow.workflowAction`. (#576)
 * Remove `runningWorkerUntilFinished`. (#589)
 * Remove key from `TypedWorker` and helpers that use it. (#606, #619)
 * Never pass an empty snapshot to `initialState`. (#556)
 * Change the return type of `Worker.finished` from `T` to `Nothing`. (#637)
 * Add an `RxWorker` class so Workers can be implemented without using experimental Flow APIs. (#650)
 * Make `RxWorker` and `Flowable.asWorker` use `Publisher` instead of `Flowable`. (#654)
 * Make it simpler to provide debugging names for workflow actions. (#696)

#### Runtime changes:

 * `launchWorkflowIn` block now takes a `WorkflowSession` instead of individual parameters. (#612)
 * Introduce `WorkflowDiagnosticListener` to support various debugging and logging tools. (#628, #634)
 * Implement chrome trace file generation. (#617)

#### Testing changes:

 * Introduce `WorkerSink` for writing integration tests that involve worker outputs. (#588)
 * Fix a race in `WorkerTester`. (#638)
 * Make testing infra run double render passes to suss out side effect code in render methods. (#678)
 * Make it possible to pass just the snapshot for the root workflow into the test methods. (#681)
 * Throw workflow exceptions from `test`/`testFromStart` instead of leaking to uncaught exception handler. (#686)

#### Android changes:

 * Eliminate `HandlesBack`, introduce `View.backPressedHandler`. (#614)
 * Introduce `WorkflowViewStub`. (#657)
 * Adds `getRendering`, `getShowRendering` `View` extensions. (#666)
 * Eliminate `WorkflowRunner.onSaveInstanceState`. (#679)
 * Compile time assurance that `BackStackScreen` is not empty. (#688)
 * Introduce `ContainerHints` for passing view-only hints around `LayoutRunner`s. (#693)

#### Version changes:

 * Upgrade Kotlin to 1.3.50. (#560)
 * Upgrade coroutines to 1.3.1 stable. (#561, #590)
 * Upgrade a bunch of other dependencies, see the commit history for details.

#### Other changes:

 * Eliminate `ExperimentalWorkflowUi`. (#565)
 * Rename `Worker<T>` to `Worker<OutputT>`. (#570)
 * Fix "java" being printed instead of class name in Rx2 Reactors. (#607)
 * Fix a worker crash introduced by `onReceiveOrClosed`. (#630)
 * Make `onEvent`, `makeActionSink`, and `makeEventSink` include what action was lost to when they receive more than one event. (#673)

### Swift

 * Update ReactiveSwift to 6.0.0. (#574 – thanks @lechristian!)
   * This also moves everything over to the standard library’s `Result` with `Never` (rather than
     `NoError`). As of this change, consumers must use Swift 5.
 * Apply child workflow output as an action in `RenderTester`. (#595)
 * Add `WorkflowUI` to `Package.swift`. (#690)

## Version 0.20.0

_2019-8-21_

### Kotlin

 * Rename `onWorkerOutput` to `runningWorker` (and friends). (#546)
 * Add Kotlin `Sink`, `makeActionSink`, `makeEventSink`. (#537)
 * Rename `LifecycleWorker.onCancelled` to `onStopped`. (#550)
 * Create a `Worker.transform` operator. (#533)
 * Remove all non-test dependencies on Kotlin Reflect. (#551)
 * Rename `InputT` -> `PropsT` (#549)
 * Update a bunch of dependency versions.

### Swift

 * Plumb file and line to convenience render tester method (#516)

## Version 0.19.0

_2019-7-29_

### Swift

 * Update to build with Swift 5.
 * Allow sinks to be reused across render passes if a sink of the same action type is declared. (#443)
 * Support updating the root workflow input on a `WorkflowHost`. (#351)
 * Add support for `RenderTester` to expect child workflows and outputs. (#442)

### Kotlin

 * `WorkflowRunner` now delivers single result instead of output stream (#468)
 * Use single object for `noAction` instead of creating it on every call (#473)
 * Upgrade AGP to 3.5.0-rc01. (#484)
 * Use Java 8 (#492)

## Version 0.18.0

_2019-7-16_

### Kotlin

#### Core API changes:

 * Added builder functions for creating stateful workflows. (#441)
 * Introduce Kotlin Flows:
   * Convert the Worker API to use Flow instead of using its own Emitter type. (#435)
   * Convert InputT streams from channels to Flows. (#433)
 * Add Completable.asWorker() operator. (#423)
 * Rename WorkflowAction.noop() to noAction(). (#420)
 * Change RenderContext.onEvent return type to be a raw function type. (#387)
 * Moves Named.key up to Compatible.compatibilityKey. (#429)

#### Runtime changes:

 * Replace WorkflowHost with the launchWorkflowIn function. (#447)
 * Delete flatMapWorkflow. (#377)
 * Allow specifying a different CoroutineDispatcher in the WorkflowRunner. (#379)

#### Version changes:

 * Bump Kotlin to latest version (1.3.41), along with Dokka and detekt. (#451)
 * Migrate to AndroidX. (#59, #469 - thanks @charbgr!)
 * Bump AGP to latest beta version (3.5.0-beta05). (#427)
 * Update coroutines dependency to 1.3.0-M2. (#409)

#### Other changes:

 * Don't throw from runningWorker when the worker finishes. (#456)
 * Less hacky lifecycle search in ModalContainer. (#448)
 * Fix Worker backpressure (too much buffering). (#446)
 * Set up a teardown hook for dialogs. (#432)
 * Artifact name changes (#419)
   * Add -jvm suffixes to all modules that could be MPP one day.
   * Add the -core suffix to the artifact for workflow-ui-core.
 * Remove (incorrect) manual implementation of ensureActive. (#378)
 * Fix WorkflowPool to work with ConflatedBroadcastChannel. (#475)

### Swift

 * Rename AnyWorkflowAction.identity to `noAction` (#444)

## Version 0.17.3

_2019-6-12_

### Kotlin

 * Breaking change: `BackStackScreen` includes the entire backstack. (#403)
 * Fixes BackStackContainer config change. (#406)
 * Fix snapshots of a non-flat tree being taken too late. (#408)

### Swift

 * Kotlin-only release, no changes.

## Version 0.17.2

_2019-6-6_

### Kotlin

 * Breaking change: `LayoutRunner` replaces `Coordinator`, no more `Scene` support in `ViewBinding`. (#383)
 * Allows WorkflowLayout to work from any stream of renderings. (#395)

### Swift

 * Kotlin-only release, no changes.

## Version 0.17.1

_2019-5-30_

### Kotlin

 * Allow specifying a different CoroutineDispatcher in the WorkflowRunner. (#379)

### Swift

 * Kotlin-only release, no changes.

## Version 0.17.0

_2019-5-29_

### Kotlin

 * Add a Worker.createSideEffect helper for Nothing-type workers. (#366)
 * Add a Worker.timer function to create simple delay workers. (#368)
 * testRender can now test arbitrary event handlers and automatically calculate initial state. (#372)
 * Allow specifying the CoroutineDispatcher used by WorkflowRunnerViewModel. (#375)

### Swift

 * `Actions` update the `Rendering` synchronously. (#362, #348)

## Version 0.16.1

_2019-5-24_

### Kotlin

 * Make RenderTester not require specific Mock* workflows/workers. (#369)
 * Breaking change: Public WorkflowLayout, tidier WorkflowRunner, bye WorkflowActivityRunner. (#367)

### Swift

 * Kotlin-only release, no changes.

## Version 0.16.0

_2019-5-22_

### Kotlin

 * Breaking change: remove the `CoroutineScope` parameter to `initialState`, remove `onTeardown`. (#289)
 * Breaking change: made `LifecycleWorker` methods non-suspending. (#328)
 * Removed must nullability restrictions from parameter types. (#334, #352)
 * Marked `workflow-ui-core` and `workflow-ui-android` APIs as experimental. (#345)
 * Breaking change: removed deprecated `Screen` classes. (#347)
 * Add experimental support for input from Flow streams. (#280)
 * Introduce alternative testing infrastructure to test single render passes. (#349)
 * Add `Emitter.emitAll` extension to consume RxJava streams from within custom workers. (#354)
 * Breaking change: simpler / richer `WorkflowActivityRunner` API. `PickledWorkflow` no longer public. (#355, #358)
 * Breaking change: make the `RenderContext` parameter to `Workflow.stateless` the receiver instead. (#357)
 * Introduces `WorkflowFragment`. (#344, #358)

### Swift

 * Add Action and Render testing helpers. (#330)

## Version 0.15.0

_2019-4-26_

### Kotlin

 * Introduce Workers as the new and only way to subscribe to external stream and future types. (#289, #321, #322, #323, #324)
 * Rename WorkflowContext to RenderContext. (#309)
 * Fix for stale workflow output handlers being invoked in later render passes. (#314)
 * `WorkflowTester` improvements:
   * Give WorkflowTester a sendInput method to update the root workflow-under-test's input. (#315)
   * Remove WorkflowTester.with* methods. (#318)
   * Make WorkflowTester the receiver of the lambdas to Workflow.test* methods. (#318)

### Swift

 * Rename `compose` to `render` and update docs. (#301)

## Version 0.14.1

_2019-4-18_

### Kotlin

 * Fix for WorkflowHost.Factory not using baseContext. ([#306](https://github.com/square/workflow/pull/306))

### Swift

 * Kotlin-only release, no code changes.

## Version 0.14.0

_2019-4-16_

### Kotlin

 * Rename WorkflowContext.compose to composeChild. [#274](https://github.com/square/workflow/issues/274)
 * Rename compose and composeChild to render and renderChild. [#293](https://github.com/square/workflow/issues/293)
 * Throw if WorkflowContext is accessed after compose returns. [#273](https://github.com/square/workflow/issues/273)
 * Pass the workflow's scope into initialState. [#286](https://github.com/square/workflow/issues/286)
 * Add the ability to update the input for a WorkflowHost. [#282](https://github.com/square/workflow/issues/282)

### Swift

 * Kotlin-only release, no code changes.

## Version 0.13.0

_2019-4-12_

### Swift

 * Don't allow `AnyScreen` to wrap itself. ([#264](https://github.com/square/workflow/pull/264))

### Kotlin

 * Upgrade Gradle to 5.x ([#113](https://github.com/square/workflow/issues/113))
 * Upgrade AGP to 3.3.0 ([#115](https://github.com/square/workflow/issues/115))

## Version 0.12.0

_2019-4-10_

### Swift

 * Improve type safety by removing AnyScreenViewController as a base type. ([#200](https://github.com/square/workflow/pull/200))
    * Replaces AnyScreenViewController with a type erased AnyScreen. This requires the compose method to output either a single typed screen, or explicitly `AnyScreen` if it may return multiple different screen types.

### Kotlin

 * Use kotlinx-coroutines-android and Dispatchers.Main instead of Rx's Android scheduler. ([#252](https://github.com/square/workflow/issues/252))
 * Upgrade okio to 2.2.2.
 * Only allocate one StatefulWorkflow per StatelessWorkflow instance.

## Version 0.11.0

_2019-4-2_

### Kotlin

 * Update Kotlin and coroutines to latest versions. ([#254](https://github.com/square/workflow/issues/254))
 * Fixed some broken kdoc links and some warnings. ([#232](https://github.com/square/workflow/issues/232))
 * Add teardown hook on `WorkflowContext`. ([#233](https://github.com/square/workflow/issues/233))
 * Rename modules:
    * `workflow-host` -> `workflow-runtime` ([#240](https://github.com/square/workflow/issues/240))
    * `viewregistry-android` -> `workflow-ui-android` ([#239](https://github.com/square/workflow/issues/239))
    * `viewregistry` -> `workflow-ui-core` ([#239](https://github.com/square/workflow/issues/239))
 * Fix broken Parcellable implementation in ModalContainer. ([#245](https://github.com/square/workflow/issues/245))
 * Introduce WorkflowActivityRunner. ([#248](https://github.com/square/workflow/issues/248))

### Swift

 * Kotlin-only release, no changes.

## Version 0.10.0

_2019-3-28_

### Kotlin

 * Factor out a common parent interface for StatelessWorkflow and Workflow (now StatefulWorkflow). ([#213](https://github.com/square/workflow/issues/213))
 * Replace restoreState with a Snapshot param to initialState. ([#220](https://github.com/square/workflow/issues/220))
 * Moves StatelessWorkflow, Workflows.kt methods to Workflow. ([#226](https://github.com/square/workflow/pull/226))

### Swift

 * Kotlin-only release, no changes.

## Version 0.9.1

_2019-3-25_

### Kotlin

 * Workaround #211 by implementing `KType` ourselves.

### Swift

 * Kotlin-only release, no changes.

## Version 0.9.0

_2019-3-22_

### Swift

 * Switch to using expectation from spinning the runloop.
 * Update ReactiveSwift to 5.0.0.
 * Added Xcode 10.2 support.
 * Add convenience extensions for makeSink and awaitResult.
 * Add xcode templates.

### Kotlin

 * Reverts Kotlin back to v1.2.61.
 * Make a StatelessWorkflow typealias and a hideState() extension function.
 * Fix the exception thrown by `WorkflowTester` when an exception is thrown inside test block.
 * Use explicit `KType` + `String` parameters instead of Any for idempotence key for subscriptions.
 * Make a `EventHandler` type to return from `makeSink`, and rename `makeSink` to `onEvent`.

## Version 0.8.1

_2019-3-15_

### Kotlin

 * Bumps Kotlin to v1.2.71.

### Swift

 * Kotlin-only release, no changes.

## Version 0.8.0

_2019-3-12_

### Kotlin

 * Breaking change, `ViewBuilder` is now `ViewBinder`. Adds `buildScene()`
   method. (#57)
 * `BackStackEffect` allows configuration of transition effects between `BackStackScreen`s.
 * `ModalContainer` adds support for `AlertDialog` and custom views in `Dialog` windows.
 * Sample app consolidated to two modules, `samples/tictactoe/android` and
   `samples/tictactoe/common`. Various `Shell*` classes in sample renamed to `Main*`.
 * Breaking change, `EventHandlingScreen` interface eliminated. It wasn't useful.
 * Breaking change, `workflow-core`, `workflow-rx2`, and `workflow-test` modules moved to legacy
   folder, given `legacy-` prefix (module and maven artifact), and code moved to `legacy` package.
   None of these modules should be used in new code, they will be deleted soon.
 * Workflows have been completely rewritten and are now almost identical to Swift workflows.

### Swift

 * Initial release.

## Version 0.7.0

_2019-1-4_

### Kotlin

 * Breaking change, further API refinement of `WorkflowPool` and friends.
   Extracts `WorkflowUpdate` and `WorkflowPool.Handle` from the defunct
   `WorkflowHandle`. (#114)

## Version 0.6.0

_2019-1-2_

### Kotlin

 * Breaking improvements to `WorkflowPool` API --
   `Delegating` interface replaced by `WorkflowHandle` sealed class. (#98)
 * Deprecates `EventSelectBuilder.onSuccess`, replaced by `onWorkflowUpdate`
   and `onWorkerResult`. (#89)
 * Sample code includes unit test of a composite workflow, `ShellReactorTest`,
   demonstrating how to keep composites decoupled from their components. (#102)
 * Adds `eventChannelOf` helper for testing. (#85, #95)

## Version 0.5.0

_2018-12-11_

### Kotlin

 * Improvements to `switchMapState`:
    * Fix: `switchMapState` now is actually a switch map, was previously behaving like a concat
      map. (#69, #71)
    * Introduced variation that takes a lambda that returns an RxJava2 `Observable`, for
      `workflow-rx2` consumers. (#53)
 * Factored the channel and coroutine management out of `ReactorWorkflow` into a more pure
   `workflow` builder function. (d0aef29)
 * Fix: Race condition in `WorkflowPool`. (#45)
 * `makeId` functions renamed to `makeWorkflowId`.
 * `ViewStackScreen` renamed to `StackScreen`.
 * Sample of using `AlertDialog` with `ViewBuilder`.
 * Improved kdoc and comments on use of `Unconfined` dispatcher. (#74, #81)
 * Workflow coroutines are now given names by default.
 * Remove some unused code. (#44, #77)

## Version 0.4.0

_2018-12-04_

### Kotlin

 * New: `ViewBuilder` – Android UI integration.
 * New: `Worker` – Helper to run async tasks via `WorkflowPool`.
 * Eliminated most of the boilerplate required by `WorkflowPool.Type`. It's now a concrete class.
 * Renamed `ComposedReactor` to `Reactor`, eliminating the old, deprecated `Reactor` interface.

## Version 0.3.0

_2018-11-28_

### Kotlin

 * `ReactorWorkflow`, `WorkflowPool`, and related types have been ported to coroutines and moved
   to the workflow-core module.

## Version 0.2.0

_2018-11-22_

### Kotlin

 * Organize everything into `com.squareup.workflow*` packages.
