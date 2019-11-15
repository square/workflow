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

#if canImport(UIKit)

import XCTest

import ReactiveSwift
import Workflow
@testable import WorkflowUI


struct TestScreen: Screen {
    var string: String
}

final class TestScreenViewController: ScreenViewController<TestScreen> {
    var onScreenChange: (() -> Void)? = nil

    override func screenDidChange(from previousScreen: TestScreen) {
        super.screenDidChange(from: previousScreen)
        onScreenChange?()
    }
}


class ContainerViewControllerTests: XCTestCase {

    let registry: ViewRegistry = {
        var registry = ViewRegistry()
        registry.register(screenViewControllerType: TestScreenViewController.self)
        return registry
    }()

    func test_initialization_renders_workflow() {
        let (signal, _) = Signal<Int, Never>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        withExtendedLifetime(container) {
            let vc = container.rootViewController as! TestScreenViewController
            XCTAssertEqual("0", vc.screen.string)
        }
    }

    func test_workflow_update_causes_rerender() {
        let (signal, observer) = Signal<Int, Never>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        withExtendedLifetime(container) {

            let expectation = XCTestExpectation(description: "View Controller updated")

            let vc = container.rootViewController as! TestScreenViewController
            vc.onScreenChange = {
                expectation.fulfill()
            }

            observer.send(value: 2)

            wait(for: [expectation], timeout: 1.0)

            XCTAssertEqual("2", vc.screen.string)
        }
    }

    func test_workflow_output_causes_container_output() {

        let (signal, observer) = Signal<Int, Never>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        let expectation = XCTestExpectation(description: "Output")

        let disposable = container.output.observeValues { value in
            XCTAssertEqual(3, value)
            expectation.fulfill()
        }

        observer.send(value: 3)

        wait(for: [expectation], timeout: 1.0)

        disposable?.dispose()
    }
}


fileprivate struct MockWorkflow: Workflow {

    var subscription: Signal<Int, Never>

    typealias State = Int

    typealias Output = Int

    func makeInitialState() -> State {
        return 0
    }

    func workflowDidChange(from previousWorkflow: MockWorkflow, state: inout State) {

    }

    func render(state: State, context: RenderContext<MockWorkflow>) -> TestScreen {

        context.subscribe(signal: subscription.map { output in
            return AnyWorkflowAction { state in
                state = output
                return output
            }
        })

        return TestScreen(string: "\(state)")
    }

}

#endif
