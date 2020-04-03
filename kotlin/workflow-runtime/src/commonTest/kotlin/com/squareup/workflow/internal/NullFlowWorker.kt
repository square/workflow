package com.squareup.workflow.internal

import com.squareup.workflow.Worker

/**
 * Worker that incorrectly returns null from [.run], to simulate the default behavior of some
 * mocking libraries.
 *
 * See [#842](https://github.com/square/workflow/issues/842).
 */
internal expect class NullFlowWorker() : Worker<Any?>
