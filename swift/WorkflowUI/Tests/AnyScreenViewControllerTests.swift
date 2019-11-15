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

#if os(iOS)

import XCTest
@testable import WorkflowUI


fileprivate struct ScreenA: Screen {}
fileprivate class ScreenAViewController: ScreenViewController<ScreenA> {}

fileprivate struct ScreenB: Screen {}
fileprivate class ScreenBViewController: ScreenViewController<ScreenB> {}


class AnyScreenViewControllerTests: XCTestCase {

    func test_update_same_screen_type_creates_one_view_controller() {
        var viewRegistry = ViewRegistry()

        viewRegistry.register(screenViewControllerType: ScreenAViewController.self)

        let viewController = AnyScreenViewController(screen: AnyScreen(ScreenA()), viewRegistry: viewRegistry)

        let expected = viewController.currentViewController

        // Render the same screen type again. The view controller should not be replaced.
        viewController.update(screen: AnyScreen(ScreenA()))

        let actual = viewController.currentViewController
        XCTAssert(expected === actual)
    }

    func test_update_generates_new_view_controller_on_different_screen_type() {
        var viewRegistry = ViewRegistry()

        viewRegistry.register(screenViewControllerType: ScreenAViewController.self)
        viewRegistry.register(screenViewControllerType: ScreenBViewController.self)

        let viewController = AnyScreenViewController(screen: AnyScreen(ScreenA()), viewRegistry: viewRegistry)

        let expected = viewController.currentViewController

        // Render a different screen type. The view controller should be replaced.
        viewController.update(screen: AnyScreen(ScreenB()))

        let actual = viewController.currentViewController
        XCTAssert(expected !== actual)
    }
}

#endif
