Change Log
==========

## Version 0.14.1

_2019-4-18_

 * Fix for WorkflowHost.Factory not using baseContext. ([#306](https://github.com/square/workflow/pull/306))

## Version 0.14.0

_2019-4-16_

 * Rename WorkflowContext.compose to composeChild. [#274](https://github.com/square/workflow/issues/274)
 * Rename compose and composeChild to render and renderChild. [#293](https://github.com/square/workflow/issues/293)
 * Throw if WorkflowContext is accessed after compose returns. [#273](https://github.com/square/workflow/issues/273)
 * Pass the workflow's scope into initialState. [#286](https://github.com/square/workflow/issues/286)
 * Add the ability to update the input for a WorkflowHost. [#282](https://github.com/square/workflow/issues/282)

## Version 0.13.0

_2019-4-12_

 * Upgrade Gradle to 5.x ([#113](https://github.com/square/workflow/issues/113))
 * Upgrade AGP to 3.3.0 ([#115](https://github.com/square/workflow/issues/115))

## Version 0.12.0

_2019-4-10_

 * Use kotlinx-coroutines-android and Dispatchers.Main instead of Rx's Android scheduler. ([#252](https://github.com/square/workflow/issues/252))
 * Upgrade okio to 2.2.2.
 * Only allocate one StatefulWorkflow per StatelessWorkflow instance.

## Version 0.11.0

_2019-4-2_

 * Update Kotlin and coroutines to latest versions. ([#254](https://github.com/square/workflow/issues/254))
 * Fixed some broken kdoc links and some warnings. ([#232](https://github.com/square/workflow/issues/232))
 * Add teardown hook on `WorkflowContext`. ([#233](https://github.com/square/workflow/issues/233))
 * Rename modules:
    * `workflow-host` -> `workflow-runtime` ([#240](https://github.com/square/workflow/issues/240))
    * `viewregistry-android` -> `workflow-ui-android` ([#239](https://github.com/square/workflow/issues/239))
    * `viewregistry` -> `workflow-ui-core` ([#239](https://github.com/square/workflow/issues/239))
 * Fix broken Parcellable implementation in ModalContainer. ([#245](https://github.com/square/workflow/issues/245))
 * Introduce WorkflowActivityRunner. ([#248](https://github.com/square/workflow/issues/248))

## Version 0.10.0

_2019-3-28_

 * Factor out a common parent interface for StatelessWorkflow and Workflow (now StatefulWorkflow). ([#213](https://github.com/square/workflow/issues/213))
 * Replace restoreState with a Snapshot param to initialState. ([#220](https://github.com/square/workflow/issues/220))
 * Moves StatelessWorkflow, Workflows.kt methods to Workflow. ([#226](https://github.com/square/workflow/pull/226))

## Version 0.9.1

_2019-3-25_

 * Workaround #211 by implementing `KType` ourselves.

## Version 0.9.0

_2019-3-22_

 * Reverts Kotlin back to v1.2.61.
 * Make a StatelessWorkflow typealias and a hideState() extension function.
 * Fix the exception thrown by `WorkflowTester` when an exception is thrown inside test block.
 * Use explicit `KType` + `String` parameters instead of Any for idempotence key for subscriptions.
 * Make a `EventHandler` type to return from `makeSink`, and rename `makeSink` to `onEvent`.

## Version 0.8.1

_2019-3-15_

 * Bumps Kotlin to v1.2.71.

## Version 0.8.0

_2019-3-12_

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

## Version 0.7.0

_2019-1-4_

 * Breaking change, further API refinement of `WorkflowPool` and friends.
   Extracts `WorkflowUpdate` and `WorkflowPool.Handle` from the defunct
   `WorkflowHandle`. (#114) 

## Version 0.6.0

_2019-1-2_

 * Breaking improvements to `WorkflowPool` API --
   `Delegating` interface replaced by `WorkflowHandle` sealed class. (#98)
 * Deprecates `EventSelectBuilder.onSuccess`, replaced by `onWorkflowUpdate`
   and `onWorkerResult`. (#89)
 * Sample code includes unit test of a composite workflow, `ShellReactorTest`,
   demonstrating how to keep composites decoupled from their components. (#102)
 * Adds `eventChannelOf` helper for testing. (#85, #95)

## Version 0.5.0

_2018-12-11_

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

 * New: `ViewBuilder` – Android UI integration.
 * New: `Worker` – Helper to run async tasks via `WorkflowPool`.
 * Eliminated most of the boilerplate required by `WorkflowPool.Type`. It's now a concrete class.
 * Renamed `ComposedReactor` to `Reactor`, eliminating the old, deprecated `Reactor` interface.

## Version 0.3.0

_2018-11-28_

 * `ReactorWorkflow`, `WorkflowPool`, and related types have been ported to coroutines and moved
   to the workflow-core module.

## Version 0.2.0

_2018-11-22_

 * Organize everything into `com.squareup.workflow*` packages.

