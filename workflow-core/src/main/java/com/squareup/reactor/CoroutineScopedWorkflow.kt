package com.squareup.reactor

import kotlinx.coroutines.experimental.CoroutineScope
import kotlin.coroutines.experimental.CoroutineContext

/**
 * A [Workflow] implementation that is also a [CoroutineScope].
 */
internal abstract class CoroutineScopedWorkflow<out S : Any, in E : Any, out O : Any>(
  final override val coroutineContext: CoroutineContext
) : Workflow<S, E, O>, CoroutineScope
