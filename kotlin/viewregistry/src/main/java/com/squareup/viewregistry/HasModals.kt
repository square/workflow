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
package com.squareup.viewregistry

/**
 * Interface implemented by screen classes that represent a stack of
 * zero or more [modal][M] screens above a [baseScreen]. Use of this
 * interface allows platform specific containers to share base classes,
 * like Android's `AbstractModalContainer`.
 */
interface HasModals<out B : Any, out M : Any> {
  val baseScreen: B
  val modals: List<M>
}
