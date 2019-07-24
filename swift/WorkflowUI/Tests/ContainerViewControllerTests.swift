import XCTest

import ReactiveSwift
import Result
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
        let (signal, _) = Signal<Int, NoError>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        withExtendedLifetime(container) {
            let vc = container.rootViewController as! TestScreenViewController
            XCTAssertEqual("0", vc.screen.string)
        }
    }

    func test_workflow_update_causes_rerender() {
        let (signal, observer) = Signal<Int, NoError>.pipe()
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

        let (signal, observer) = Signal<Int, NoError>.pipe()
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

    func test_updating_workflow_causes_render_update() {
        let (signal, _) = Signal<Int, NoError>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        withExtendedLifetime(container) {

            let expectation = XCTestExpectation(description: "View Controller updated")

            let vc = container.rootViewController as! TestScreenViewController
            vc.onScreenChange = {
                expectation.fulfill()
            }

            container.update(workflow: MockWorkflow(subscription: signal))

            wait(for: [expectation], timeout: 1.0)

            XCTAssertEqual("10", vc.screen.string)
        }
    }
}


fileprivate struct MockWorkflow: Workflow {

    var subscription: Signal<Int, NoError>

    typealias State = Int

    typealias Output = Int

    func makeInitialState() -> State {
        return 0
    }

    func workflowDidChange(from previousWorkflow: MockWorkflow, state: inout State) {
        state = 10
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
