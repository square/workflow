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

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.ui.backstack.BackStackScreen
import org.junit.Test

class OverviewDetailScreenTest {

  @Test fun `minimal structure`() {
    val screen = OverviewDetailScreen(BackStackScreen(1))
    assertThat(screen.overviewRendering).isEqualTo(BackStackScreen(1))
    assertThat(screen.detailRendering).isNull()
    assertThat(screen.selectDefault).isNull()
  }

  @Test fun `minimal equality`() {
    val screen = OverviewDetailScreen(BackStackScreen(1))
    assertThat(screen).isEqualTo(OverviewDetailScreen(BackStackScreen(1)))
    assertThat(screen).isNotEqualTo(OverviewDetailScreen(BackStackScreen(2)))
  }

  @Test fun `minimal hash`() {
    val screen = OverviewDetailScreen(BackStackScreen(1))
    assertThat(screen.hashCode()).isEqualTo(OverviewDetailScreen(BackStackScreen(1)).hashCode())
    assertThat(screen.hashCode()).isNotEqualTo(OverviewDetailScreen(BackStackScreen(2)).hashCode())
  }

  @Test fun `combine minimal`() {
    val left = OverviewDetailScreen(BackStackScreen(1, 2))
    val right = OverviewDetailScreen(BackStackScreen(11, 12))

    assertThat(left + right).isEqualTo(OverviewDetailScreen(BackStackScreen(1, 2, 11, 12)))
  }

  @Test fun `full structure`() {
    val screen = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4)
    )

    assertThat(screen.overviewRendering).isEqualTo(BackStackScreen(1, 2))
    assertThat(screen.detailRendering).isEqualTo(BackStackScreen(3, 4))
    assertThat(screen.selectDefault).isNull()
  }

  @Test fun `full equality`() {
    val screen1 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4)
    )

    val screen2 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4)
    )

    val screen3 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4, 5)
    )

    assertThat(screen1).isEqualTo(screen2)
    assertThat(screen1).isNotEqualTo(screen3)
  }

  @Test fun `full hash`() {
    val screen1 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4)
    )

    val screen2 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4)
    )

    val screen3 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4, 5)
    )

    assertThat(screen1.hashCode()).isEqualTo(screen2.hashCode())
    assertThat(screen1.hashCode()).isNotEqualTo(screen3.hashCode())
  }

  @Test fun `combine full`() {
    val left = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        detailRendering = BackStackScreen(3, 4)
    )
    val right = OverviewDetailScreen(
        overviewRendering = BackStackScreen(11, 12),
        detailRendering = BackStackScreen(13, 14)
    )

    assertThat(left + right).isEqualTo(
        OverviewDetailScreen(
            overviewRendering = BackStackScreen(1, 2, 11, 12),
            detailRendering = BackStackScreen(3, 4, 13, 14)
        )
    )
  }

  @Test fun `selectDefault structure`() {
    val selectDefault = {}
    val screen = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = selectDefault
    )

    assertThat(screen.overviewRendering).isEqualTo(BackStackScreen(1, 2))
    assertThat(screen.detailRendering).isNull()
    assertThat(screen.selectDefault).isEqualTo(selectDefault)
  }

  @Test fun `selectDefault equality`() {
    val selectDefault = {}

    val screen1 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = selectDefault
    )

    val screen2 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = selectDefault
    )

    val screen3 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = {}
    )

    assertThat(screen1).isEqualTo(screen2)
    assertThat(screen1).isNotEqualTo(screen3)
  }

  @Test fun `selectDefault hash`() {
    val selectDefault = {}

    val screen1 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = selectDefault
    )

    val screen2 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = selectDefault
    )

    val screen3 = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = {}
    )

    assertThat(screen1.hashCode()).isEqualTo(screen2.hashCode())
    assertThat(screen1.hashCode()).isNotEqualTo(screen3.hashCode())
  }

  @Test fun `combine selectDefault`() {
    val selectDefaultLeft = {}
    val left = OverviewDetailScreen(
        overviewRendering = BackStackScreen(1, 2),
        selectDefault = selectDefaultLeft
    )
    val selectDefaultRight = {}
    val right = OverviewDetailScreen(
        overviewRendering = BackStackScreen(11, 12),
        selectDefault = selectDefaultRight
    )

    assertThat(left + right).isEqualTo(
        OverviewDetailScreen(
            overviewRendering = BackStackScreen(1, 2, 11, 12),
            selectDefault = selectDefaultRight
        )
    )
  }
}
