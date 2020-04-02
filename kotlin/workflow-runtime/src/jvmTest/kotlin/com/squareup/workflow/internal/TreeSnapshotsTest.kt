/*
 * Copyright 2019 Square Inc.
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
package com.squareup.workflow.internal

import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TreeSnapshotsTest {

  @Test fun `serialize and deserialize`() {
    val rootSnapshot = Snapshot.of("roo")
    val childSnapshots = listOf(
        WorkflowId(Workflow1::class) to Snapshot.of("one"),
        WorkflowId(Workflow2::class) to Snapshot.of("two"),
        WorkflowId(Workflow2::class, name = "b") to Snapshot.of("three")
    )

    val treeSnapshot = createTreeSnapshot(rootSnapshot, childSnapshots)
    val (restoredRoot, restoredChildren) = parseTreeSnapshot(treeSnapshot.bytes)

    assertEquals(rootSnapshot.bytes, restoredRoot)
    assertEquals(Workflow1::class, restoredChildren[0].first.type)
    assertEquals(Workflow2::class, restoredChildren[1].first.type)
    assertEquals(Workflow2::class, restoredChildren[2].first.type)

    assertEquals("", restoredChildren[0].first.name)
    assertEquals("", restoredChildren[1].first.name)
    assertEquals("b", restoredChildren[2].first.name)

    assertEquals("one", restoredChildren[0].second.utf8())
    assertEquals("two", restoredChildren[1].second.utf8())
    assertEquals("three", restoredChildren[2].second.utf8())
  }

  @Test fun `deserialize empty root returns null`() {
    val rootSnapshot = Snapshot.EMPTY

    val treeSnapshot = createTreeSnapshot(rootSnapshot, emptyList())
    val (restoredRoot, _) = parseTreeSnapshot(treeSnapshot.bytes)

    assertNull(restoredRoot)
  }
}

private interface Workflow1 : Workflow<Unit, Nothing, Unit>
private interface Workflow2 : Workflow<Unit, Nothing, Unit>
