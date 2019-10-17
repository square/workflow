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

import com.squareup.sample.poetry.model.Poet.Poe

val ToHelen = Poem(
    title = "To Helen",
    poet = Poe,
    stanzas = listOf(
        listOf(
            "Helen, thy beauty is to me",
            "\tLike those Nicéan barks of yore,",
            "That gently, o'er a perfumed sea,",
            "\tThe weary, way-worn wanderer bore",
            "\tTo his own native shore."
        ),
        listOf(
            "On desperate seas long wont to roam,",
            "\tThy hyacinth hair, thy classic face,",
            "Thy Naiad airs have brought me home",
            "\tTo the glory that was Greece,",
            "\tAnd the grandeur that was Rome."
        ),
        listOf(
            "Lo! in yon brilliant window-niche",
            "\tHow statue-like I see thee stand,",
            "The agate lamp within thy hand!",
            "\tAh, Psyche, from the regions which",
            "\tAre Holy-Land!"
        )
    )
)
