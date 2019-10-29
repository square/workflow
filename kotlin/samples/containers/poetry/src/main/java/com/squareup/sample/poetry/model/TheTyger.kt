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
package com.squareup.sample.poetry.model

import com.squareup.sample.poetry.model.Poet.Blake

val TheTyger = Poem(
    title = "The Tyger",
    poet = Blake,
    stanzas = listOf(
        listOf(
            "Tyger Tyger, burning bright,",
            "In the forests of the night;",
            "What immortal hand or eye,",
            "Could frame thy fearful symmetry?"
        ),
        listOf(
            "In what distant deeps or skies.",
            "Burnt the fire of thine eyes?",
            "On what wings dare he aspire?",
            "What the hand, dare seize the fire?"
        ),
        listOf(
            "And what shoulder, & what art,",
            "Could twist the sinews of thy heart?",
            "And when thy heart began to beat,",
            "What dread hand? & what dread feet?"
        ),
        listOf(
            "What the hammer? what the chain,",
            "In what furnace was thy brain?",
            "What the anvil? what dread grasp,",
            "Dare its deadly terrors clasp!"
        ),
        listOf(
            "When the stars threw down their spears",
            "And water'd heaven with their tears:",
            "Did he smile his work to see?",
            "Did he who made the Lamb make thee?"
        ),
        listOf(
            "Tyger Tyger burning bright,",
            "In the forests of the night:",
            "What immortal hand or eye,",
            "Dare frame thy fearful symmetry?"
        )
    )
)
