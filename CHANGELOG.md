Change Log
==========

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

