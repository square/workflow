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
package com.squareup.sample.container.overviewdetail

import com.squareup.workflow.ui.backstack.BackStackScreen

/**
 * Rendering type for overview / detail containers, with [BackStackScreen] in both roles.
 *
 * Containers may choose to display both children side by side in a split view, or concatenate them
 * (overview + detail) in a single pane.
 *
 * @param selectDefault optional function that a split view container may call to request
 * that a selection be made to fill a null [detailRendering].
 */
class OverviewDetailScreen private constructor(
  val overviewRendering: BackStackScreen<Any>,
  val detailRendering: BackStackScreen<Any>? = null,
  val selectDefault: (() -> Unit)? = null
) {
  constructor(
    overviewRendering: BackStackScreen<Any>,
    detailRendering: BackStackScreen<Any>
  ) : this(overviewRendering, detailRendering, null)

  /**
   * @param selectDefault optional function that a split view container may call to request
   * that a selection be made to fill a null [detailRendering].
   */
  constructor(
    overviewRendering: BackStackScreen<Any>,
    selectDefault: (() -> Unit)? = null
  ) : this(overviewRendering, null, selectDefault)

  operator fun component1(): BackStackScreen<Any> = overviewRendering
  operator fun component2(): BackStackScreen<Any>? = detailRendering

  /**
   * Returns a new [OverviewDetailScreen] appending the [overviewRendering] and
   * [detailRendering] of [other] to those of the receiver. If the new screen's
   * [detailRendering] is `null`, it will have the [selectDefault] function of [other].
   */
  operator fun plus(other: OverviewDetailScreen): OverviewDetailScreen {
    val newOverview = overviewRendering + other.overviewRendering
    val newDetail = detailRendering
        ?.let { it + other.detailRendering }
        ?: other.detailRendering

    return if (newDetail == null) OverviewDetailScreen(newOverview, other.selectDefault)
    else OverviewDetailScreen(newOverview, newDetail)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as OverviewDetailScreen

    return overviewRendering == other.overviewRendering &&
        detailRendering == other.detailRendering &&
        selectDefault == other.selectDefault
  }

  override fun hashCode(): Int {
    var result = overviewRendering.hashCode()
    result = 31 * result + (detailRendering?.hashCode() ?: 0)
    result = 31 * result + (selectDefault?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "OverviewDetailScreen(overviewRendering=$overviewRendering, " +
        "detailRendering=$detailRendering, " +
        "selectDefault=$selectDefault)"
  }
}
