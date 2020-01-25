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
package com.squareup.sample.container.masterdetail

import com.squareup.workflow.ui.backstack.BackStackScreen

/**
 * Rendering type for master / detail containers, with [BackStackScreen] in both roles.
 *
 * Containers may choose to display both children side by side in a split view, or concatenate them
 * (master + detail) in a single pane.
 *
 * @param selectDefault optional function that a split view container may call to request
 * that a selection be made to fill a null [detailRendering].
 */
class MasterDetailScreen private constructor(
  val masterRendering: BackStackScreen<Any>,
  val detailRendering: BackStackScreen<Any>? = null,
  val selectDefault: (() -> Unit)? = null
) {
  constructor(
    masterRendering: BackStackScreen<Any>,
    detailRendering: BackStackScreen<Any>
  ) : this(masterRendering, detailRendering, null)

  /**
   * @param selectDefault optional function that a split view container may call to request
   * that a selection be made to fill a null [detailRendering].
   */
  constructor(
    masterRendering: BackStackScreen<Any>,
    selectDefault: (() -> Unit)? = null
  ) : this(masterRendering, null, selectDefault)

  operator fun component1(): BackStackScreen<Any> = masterRendering
  operator fun component2(): BackStackScreen<Any>? = detailRendering

  /**
   * Returns a new [MasterDetailScreen] appending the [masterRendering] and
   * [detailRendering] of [other] to those of the receiver. If the new screen's
   * [detailRendering] is `null`, it will have the [selectDefault] function of [other].
   */
  operator fun plus(other: MasterDetailScreen): MasterDetailScreen {
    val newMaster = masterRendering + other.masterRendering
    val newDetail = detailRendering
        ?.let { it + other.detailRendering }
        ?: other.detailRendering

    return if (newDetail == null) MasterDetailScreen(newMaster, other.selectDefault)
    else MasterDetailScreen(newMaster, newDetail)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MasterDetailScreen

    return masterRendering == other.masterRendering &&
        detailRendering == other.detailRendering &&
        selectDefault == other.selectDefault
  }

  override fun hashCode(): Int {
    var result = masterRendering.hashCode()
    result = 31 * result + (detailRendering?.hashCode() ?: 0)
    result = 31 * result + (selectDefault?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "MasterDetailScreen(masterRendering=$masterRendering, " +
        "detailRendering=$detailRendering, " +
        "selectDefault=$selectDefault)"
  }
}
