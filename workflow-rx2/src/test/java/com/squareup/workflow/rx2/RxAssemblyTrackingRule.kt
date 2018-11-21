package com.squareup.workflow.rx2

import hu.akarnokd.rxjava2.debug.RxJavaAssemblyTracking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Invokes [JavaHooks.enableAssemblyTracking()] for the duration of this test,
 * for easier debugging of exceptions caught by Rx.
 */
class RxAssemblyTrackingRule : TestRule {
  override fun apply(
    base: Statement,
    description: Description
  ): Statement {
    return object : Statement() {
      override fun evaluate() {
        RxJavaAssemblyTracking.enable()
        try {
          base.evaluate()
        } finally {
          RxJavaAssemblyTracking.disable()
        }
      }
    }
  }
}
