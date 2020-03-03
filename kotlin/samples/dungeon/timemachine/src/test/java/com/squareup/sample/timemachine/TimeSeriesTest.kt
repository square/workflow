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
package com.squareup.sample.timemachine

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
class TimeSeriesTest {

  private val series = TimeSeries<String>()

  @Test fun `duration is initially zero`() {
    assertThat(series.duration).isEqualTo(Duration.ZERO)
  }

  @Test fun `duration increases after append`() {
    series.append("foo", 42.milliseconds)
        .let {
          assertThat(it.duration).isEqualTo(42.milliseconds)
        }
  }

  @Test fun `duration increases after multiple appends`() {
    series.append("foo", 2.milliseconds)
        .append("bar", 42.milliseconds)
        .let {
          assertThat(it.duration).isEqualTo(42.milliseconds)
        }
  }

  @Test fun `throws when appending value from the past`() {
    val series1 = series.append("foo", 42.milliseconds)

    assertFailsWith<IllegalArgumentException> {
      series1.append("bar", 41.milliseconds)
    }
  }

  @Test fun `allows appending value with last timestamp`() {
    series.append("foo", 42.milliseconds)
        .append("bar", 42.milliseconds)
        .let {
          assertThat(it.duration).isEqualTo(42.milliseconds)
        }
  }

  @Test fun `findValueNearest with empty list`() {
    assertFailsWith<NoSuchElementException> {
      series.findValueNearest(42.milliseconds)
    }
  }

  @Test fun `findValueNearest with single value`() {
    series.append("foo", 42.milliseconds)
        .let {
          assertThat(it.findValueNearest(0.milliseconds)).isEqualTo("foo")
          assertThat(it.findValueNearest(42.milliseconds)).isEqualTo("foo")
          assertThat(it.findValueNearest(100.days)).isEqualTo("foo")
        }
  }

  @Test fun `findValueNearest with multiple values`() {
    series.append("foo", 41.milliseconds)
        .append("bar", 43.milliseconds)
        .let {
          assertThat(it.findValueNearest(0.milliseconds)).isEqualTo("foo")
          assertThat(it.findValueNearest(41.milliseconds)).isEqualTo("foo")
          assertThat(it.findValueNearest(42.milliseconds)).isEqualTo("foo")
          assertThat(it.findValueNearest(43.milliseconds)).isEqualTo("bar")
          assertThat(it.findValueNearest(100.days)).isEqualTo("bar")
        }
  }
}
