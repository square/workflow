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
package com.squareup.workflow

import kotlin.RequiresOptIn.Level.ERROR
import kotlin.annotation.AnnotationRetention.BINARY

/**
 * Marks Workflow APIs that are extremely likely to change in future versions, rely themselves on
 * other unstable, experimental APIs, and SHOULD NOT be used in production code. Proceed with
 * caution, and be ready to have the rug pulled out from under you.
 */
@MustBeDocumented
@Retention(value = BINARY)
@RequiresOptIn(level = ERROR)
annotation class VeryExperimentalWorkflow
