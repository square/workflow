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

#if canImport(UIKit)

    /// Screens are the building blocks of an interactive application.
    ///
    /// Conforming types contain any information needed to populate a screen: data,
    /// styling, event handlers, etc.
    public protocol Screen {
        /// A view controller description that acts as a recipe to either build
        /// or update a previously-built view controller to match this screen.
        func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription
    }

#endif
