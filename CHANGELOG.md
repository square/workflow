Change Log
==========

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

