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
package com.squareup.workflow.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

// If you try to replace isTrue() with isTrue compilation fails.
@Suppress("UsePropertyAccessSyntax")
class NamedTest {
  object Whut
  object Hey

  @Test fun `same type same name matches`() {
    assertThat(compatible(Named(Hey, "eh"), Named(Hey, "eh"))).isTrue()
  }

  @Test fun `same type diff name matches`() {
    assertThat(compatible(Named(Hey, "blam"), Named(Hey, "bloom"))).isFalse()
  }

  @Test fun `diff type same name no match`() {
    assertThat(compatible(Named(Hey, "a"), Named(Whut, "a"))).isFalse()
  }

  @Test fun recursion() {
    assertThat(
        compatible(
            Named(Named(Hey, "one"), "ho"),
            Named(Named(Hey, "one"), "ho")
        )
    ).isTrue()

    assertThat(
        compatible(
            Named(Named(Hey, "one"), "ho"),
            Named(Named(Hey, "two"), "ho")
        )
    ).isFalse()

    assertThat(
        compatible(
            Named(Named(Hey, "a"), "ho"),
            Named(Named(Whut, "a"), "ho")
        )
    ).isFalse()
  }

  @Test fun keyRecursion() {
    assertThat(Named(Named(Hey, "one"), "ho").key)
        .isEqualTo(Named(Named(Hey, "one"), "ho").key)

    assertThat(Named(Named(Hey, "one"), "ho").key)
        .isNotEqualTo(Named(Named(Hey, "two"), "ho").key)

    assertThat(Named(Named(Hey, "a"), "ho").key)
        .isNotEqualTo(Named(Named(Whut, "a"), "ho").key)
  }

  @Test fun recursiveKeysAreLegible() {
    assertThat(Named(Named(Hey, "one"), "ho").key)
        .isEqualTo("com.squareup.workflow.ui.NamedTest\$Hey-Named(one)-Named(ho)")
  }
}
