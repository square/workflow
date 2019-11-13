package com.squareup.workflow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerTest {

  @Test fun `default Worker#doesSameWorkAs implementation compares by concrete type`() {
    class Worker1 : Worker<Nothing> {
      override fun run(): Flow<Nothing> = emptyFlow()
    }

    class Worker2 : Worker<Nothing> {
      override fun run(): Flow<Nothing> = emptyFlow()
    }

    assertTrue(Worker1().doesSameWorkAs(Worker1()))
    assertFalse(Worker1().doesSameWorkAs(Worker2()))
  }
}
