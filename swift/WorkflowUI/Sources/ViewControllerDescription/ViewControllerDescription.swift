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

    /// A ViewControllerDescription acts as a recipe for building and updating a
    /// specific UIViewController.
    public struct ViewControllerDescription {
        private let viewControllerType: UIViewController.Type
        private let build: () -> UIViewController
        private let update: (UIViewController) -> Void

        /// Constructs a view controller description by providing a closure used to
        /// build and update a specific view controller type.
        ///
        /// - Parameters:
        ///   - type: The type of view controller produced by this description.
        ///           Typically, should should be able to omit this parameter, but
        ///           in cases where type inference has trouble, it’s offered as
        ///           an escape hatch.
        ///   - build: Closure that produces a new instance of the view controller
        ///   - update: Closure that updates the given view controller
        public init<VC: UIViewController>(type: VC.Type = VC.self, build: @escaping () -> VC, update: @escaping (VC) -> Void) {
            self.viewControllerType = type
            self.build = build
            self.update = { untypedViewController in
                guard let viewController = untypedViewController as? VC else {
                    fatalError("Unable to update \(untypedViewController), expecting a \(VC.self)")
                }
                update(viewController)
            }
        }

        /// Construct and update a new view controller as described by this view
        /// controller description.
        internal func buildViewController() -> UIViewController {
            let viewController = build()
            assert(canUpdate(viewController: viewController), "View controller description built a view controller it cannot update (\(viewController) is not exactly type \(viewControllerType))")

            // Perform an initial update of the built view controller
            update(viewController: viewController)

            return viewController
        }

        /// If the given view controller is of the correct type to be updated by
        /// this view controller description.
        internal func canUpdate(viewController: UIViewController) -> Bool {
            return type(of: viewController) == viewControllerType
        }

        /// Update the given view controller.
        ///
        /// - Note: Passing a view controller that does not return `true` from
        ///         `canUpdate(viewController:)` will result in an exception.
        ///
        /// - Parameter viewController: The view controller instance to update
        internal func update(viewController: UIViewController) {
            update(viewController)
        }
    }

#endif
