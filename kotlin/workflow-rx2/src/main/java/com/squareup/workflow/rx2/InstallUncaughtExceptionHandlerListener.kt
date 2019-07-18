@file:JvmName("InstallUncaughtExceptionHandlerListener")

package com.squareup.workflow.rx2

import org.junit.runner.Description
import org.junit.runner.notification.RunListener

class InstallUncaughtExceptionHandlerListener : RunListener() {
  override fun testRunStarted(description: Description?) {
    Thread.setDefaultUncaughtExceptionHandler { _, e -> throw(e) }
  }
}
