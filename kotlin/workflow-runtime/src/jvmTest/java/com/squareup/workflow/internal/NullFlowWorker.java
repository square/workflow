package com.squareup.workflow.internal;

import com.squareup.workflow.Worker;
import kotlinx.coroutines.flow.Flow;
import org.jetbrains.annotations.NotNull;

/**
 * Worker that incorrectly returns null from {@link #run}, to simulate the default behavior of some
 * mocking libraries.
 *
 * See <a href="https://github.com/square/workflow/issues/842">#842</a>.
 */
class NullFlowWorker implements Worker {
  @NotNull @Override public Flow run() {
    //noinspection ConstantConditions
    return null;
  }

  @Override public boolean doesSameWorkAs(@NotNull Worker otherWorker) {
    //noinspection unchecked
    return Worker.DefaultImpls.doesSameWorkAs(this, otherWorker);
  }

  /**
   * Override this to make writing assertions on exception messages easier.
   */
  @Override public String toString() {
    return "NullFlowWorker.toString";
  }
}
