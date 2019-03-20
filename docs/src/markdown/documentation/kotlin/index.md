---
title: Kotlin
index: 4
navigation:
    visible: true
    path: documentation
---

# Kotlin

The Workflow infrastructure is split into several modules.

## Non-UI

### `workflow-core`

The `workflow-core` library contains the core types that are used to implement state-driven workflows, including the `Workflow` interface.

Important types defined in this module are:
 - `Workflow`
 - `StatelessWorkflow`
 - `WorkflowAction`
 - `WorkflowContext`
 - `Snapshot`

### `workflow-host`

The `workflow-host` library contains the logic that actually drives a tree of workflows, and provides external access to the outputs from the root workflow through a `WorkflowHost`.

You only need to depend on this module if you're writing integration code to host a workflow-driven app.

### `workflow-rx2`

The `workflow-rx2` module contains adapter code to make RxJava2 more convenient to use with workflows.

### `workflow-testing`

The `workflow-testing` contains helpers for testing workflows. This module should only be depended on from test targets (e.g. `testImplementation` in Gradle).

## UI

### `viewregistry`

TK

### `viewregistry-android`

TK

---

Workflow for Kotlin makes extensive use of [coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html), however most coroutine code is hidden you don't have to know much about coroutines to use this library.