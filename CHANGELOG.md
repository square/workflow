Change Log
==========

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

