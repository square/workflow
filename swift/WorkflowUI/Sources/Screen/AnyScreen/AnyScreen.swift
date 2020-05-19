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

    import UIKit

    public struct AnyScreen: Screen {
        /// The original screen, retained for debugging
        internal let wrappedScreen: Screen

        /// Stored getter for the wrapped screen’s view controller description
        private let _viewControllerDescription: (ViewEnvironment) -> ViewControllerDescription

        public init<T: Screen>(_ screen: T) {
            if let anyScreen = screen as? AnyScreen {
                self = anyScreen
                return
            }
            self.wrappedScreen = screen
            self._viewControllerDescription = screen.viewControllerDescription(environment:)
        }

        public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
            // Passed straight through
            return _viewControllerDescription(environment)
        }
    }

    extension Screen {
        /// Wraps the screen in an AnyScreen
        public func asAnyScreen() -> AnyScreen {
            AnyScreen(self)
        }
    }

#endif
