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
package com.squareup.sample.gameworkflow

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PlayerInfoTest : StringSpec({
  "read write turn" {
    val before = PlayerInfo("able", "baker")
    val out = before.toSnapshot()
    val after = PlayerInfo.fromSnapshot(out.bytes)

    after shouldBe before
  }
})
