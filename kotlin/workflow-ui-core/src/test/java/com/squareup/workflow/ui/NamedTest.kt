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

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.AnnotationSpec

class NamedTest : AnnotationSpec() {
  object Whut
  object Hey

  @Test fun `same type same name matches`() {
    compatible(Named(Hey, "eh"), Named(Hey, "eh")) shouldBe true
  }

  @Test fun `same type diff name matches`() {
    compatible(Named(Hey, "blam"), Named(Hey, "bloom")) shouldBe false
  }

  @Test fun `diff type same name no match`() {
    compatible(Named(Hey, "a"), Named(Whut, "a")) shouldBe false
  }

  @Test fun recursion() {

    compatible(Named(Named(Hey, "one"), "ho"), Named(Named(Hey, "one"), "ho")) shouldBe true

    compatible(Named(Named(Hey, "one"), "ho"), Named(Named(Hey, "two"), "ho")) shouldBe false

    compatible(Named(Named(Hey, "a"), "ho"), Named(Named(Whut, "a"), "ho")) shouldBe false
  }

  @Test fun `key recursion`() {
    Named(Named(Hey, "one"), "ho").compatibilityKey shouldBe
        Named(Named(Hey, "one"), "ho").compatibilityKey

    Named(Named(Hey, "one"), "ho").compatibilityKey shouldNotBe
        Named(Named(Hey, "two"), "ho").compatibilityKey

    Named(Named(Hey, "a"), "ho").compatibilityKey shouldNotBe
        Named(Named(Whut, "a"), "ho").compatibilityKey
  }

  @Test fun `recursive keys are legible`() {
    Named(Named(Hey, "one"), "ho").compatibilityKey shouldBe
        "com.squareup.workflow.ui.NamedTest\$Hey-Named(one)-Named(ho)"
  }

  private class Foo(override val compatibilityKey: String) : Compatible

  @Test fun `the test Compatible class actually works`() {
    compatible(Foo("bar"), Foo("bar")) shouldBe true
    compatible(Foo("bar"), Foo("baz")) shouldBe false
  }

  @Test fun `wrapping custom Compatible compatibility works`() {
    compatible(Named(Foo("bar"), "name"), Named(Foo("bar"), "name")) shouldBe true
    compatible(Named(Foo("bar"), "name"), Named(Foo("baz"), "name")) shouldBe false
  }

  @Test fun `wrapping custom Compatible keys work`() {
    Named(Foo("bar"), "name").compatibilityKey shouldBe Named(Foo("bar"), "name").compatibilityKey
    Named(Foo("bar"), "name").compatibilityKey shouldNotBe
        Named(Foo("baz"), "name").compatibilityKey
  }
}
