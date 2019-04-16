Change Log
==========

## Version 0.14.0

_2019-4-16_

 * Kotlin-only release, no code changes.

## Version 0.13.0

_2019-4-12_

 * Don't allow `AnyScreen` to wrap itself. ([#264](https://github.com/square/workflow/pull/264))

## Version 0.12.0

_2019-4-10_

 * Improve type safety by removing AnyScreenViewController as a base type. ([#200](https://github.com/square/workflow/pull/200))
    * Replaces AnyScreenViewController with a type erased AnyScreen. This requires the compose method to output either a single typed screen, or explicitly `AnyScreen` if it may return multiple different screen types.

## Version 0.11.0

_2019-4-2_

 * Kotlin-only release, no changes.

## Version 0.10.0

_2019-3-28_

 * Kotlin-only release, no changes.

## Version 0.9.1

_2019-3-25_

 * Kotlin-only release, no changes.

## Version 0.9.0

_2019-3-22_

 * Switch to using expectation from spinning the runloop.
 * Update ReactiveSwift to 5.0.0.
 * Added Xcode 10.2 support.
 * Add convenience extensions for makeSink and awaitResult.
 * Add xcode templates.

## Version 0.8.1

_2019-3-15_

 * Kotlin-only release, no changes.

## Version 0.8.0

_2019-3-12_

 * Kotlin-only release, no changes.

## Version 0.1.0

_2019-3-13_

 * Initial commit
 