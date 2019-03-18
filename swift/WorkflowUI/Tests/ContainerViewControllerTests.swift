import XCTest

import ReactiveSwift
import Result
import Workflow
@testable import WorkflowUI


struct TestScreen: Screen {
    var string: String
}

class TestScreenViewController: ScreenViewController<TestScreen> {}


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
            let vc = container.renderer.currentScreenViewController as! TestScreenViewController
            XCTAssertEqual("0", vc.screen.string)
        }
    }

    func test_workflow_update_causes_rerender() {
        let (signal, observer) = Signal<Int, NoError>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        withExtendedLifetime(container) {
            observer.send(value: 2)

            RunLoop.current.run(until: Date().addingTimeInterval(0.1))

            let vc = container.renderer.currentScreenViewController as! TestScreenViewController
            XCTAssertEqual("2", vc.screen.string)
        }
    }

    func test_workflow_output_causes_container_output() {

        let (signal, observer) = Signal<Int, NoError>.pipe()
        let workflow = MockWorkflow(subscription: signal)
        let container = ContainerViewController(workflow: workflow, viewRegistry: registry)

        var outputCalled = false
        let disposable = container.output.observeValues { value in
            XCTAssertEqual(3, value)
            outputCalled = true
        }

        observer.send(value: 3)
        RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        XCTAssertTrue(outputCalled)
        disposable?.dispose()
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

    }

    func compose(state: State, context: WorkflowContext<MockWorkflow>) -> Screen {

        context.subscribe(signal: subscription.map { output in
            return AnyWorkflowAction { state in
                state = output
                return output
            }
        })

        return TestScreen(string: "\(state)")
    }

}
