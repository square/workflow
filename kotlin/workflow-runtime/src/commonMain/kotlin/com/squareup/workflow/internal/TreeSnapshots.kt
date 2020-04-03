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
import com.squareup.workflow.parse
import com.squareup.workflow.readByteStringWithLength
import com.squareup.workflow.readList
import com.squareup.workflow.writeByteStringWithLength
import com.squareup.workflow.writeList
import okio.ByteString

/**
 * Returns a [Snapshot] that lazily aggregates a root snapshot and the snapshots of all its children
 * into a single [ByteString].
 *
 * The component snapshots can be restored with [parseTreeSnapshot].
 *
 * @see parseTreeSnapshot
 */
internal fun createTreeSnapshot(
  rootSnapshot: Snapshot,
  childSnapshots: List<Pair<AnyId, Snapshot>>
): Snapshot = Snapshot.write { sink ->
  sink.writeByteStringWithLength(rootSnapshot.bytes)
  sink.writeList(childSnapshots) { (childId, childSnapshot) ->
    writeByteStringWithLength(childId.toByteString())
    writeByteStringWithLength(childSnapshot.bytes)
  }
}

/**
 * Parses a "root" snapshot and the list of child snapshots with associated [WorkflowId]s from a
 * [ByteString] returned by [createTreeSnapshot].
 *
 * Never returns an empty root snapshot: if the root snapshot is empty it will be null.
 * Child snapshots, however, are always returned as-is. They must be recursively passed to this
 * function to continue parsing the tree.
 *
 * @see createTreeSnapshot
 */
internal fun parseTreeSnapshot(
  treeSnapshotBytes: ByteString
): Pair<ByteString?, List<Pair<AnyId, ByteString>>> = treeSnapshotBytes.parse { source ->
  val rootSnapshot = source.readByteStringWithLength()
  val childSnapshots = source.readList {
    val id = restoreId(readByteStringWithLength())
    val childSnapshot = readByteStringWithLength()
    return@readList Pair(id, childSnapshot)
  }
  // Empty snapshot means no snapshot.
  val nonEmptyRootSnapshot = rootSnapshot.takeIf { it.size > 0 }
  return Pair(nonEmptyRootSnapshot, childSnapshots)
}
