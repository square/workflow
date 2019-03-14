/*
 * Copyright 2017 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.legacy.rx2

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
