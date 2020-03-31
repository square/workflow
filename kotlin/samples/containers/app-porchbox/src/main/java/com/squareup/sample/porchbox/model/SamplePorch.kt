/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.porchbox.model

import android.net.Uri

/**
 * Simple static [Porch] using placeholder images from Lorem Picsum.
 */
val SamplePorch = Porch(
    inbox = Inbox(
        listOf(
            Item(
                name = "Groceries",
                photoUri = Uri.parse("https://picsum.photos/id/1021/200/200.jpg"),
                index = 0
            ),
            Item(
                name = "Lysol Wipes",
                photoUri = Uri.parse("https://picsum.photos/id/1022/200/200.jpg"),
                index = 1
            ),
            Item(
                name = "Bike Pump",
                photoUri = Uri.parse("https://picsum.photos/id/1023/200/200.jpg"),
                index = 2
            ),
            Item(
                name = "Power Drill",
                photoUri = Uri.parse("https://picsum.photos/id/1024/200/200.jpg"),
                index = 3
            )
        )
    )
)