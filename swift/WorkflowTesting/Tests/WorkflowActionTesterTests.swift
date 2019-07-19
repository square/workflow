import XCTest
import ReactiveSwift
import Workflow
@testable import WorkflowTesting

final class WorkflowActionTesterTests: XCTestCase {

    func test_stateTransitions() {
        TestAction
            .tester(withState: false)
            .assertState { XCTAssertFalse($0) }
            .send(action: .toggleTapped)
            .assertState { XCTAssertTrue($0) }
    }

    func test_outputs() {
        TestAction
            .tester(withState: false)
            .send(action: .exitTapped) { output in
                XCTAssertEqual(output, .finished)
            }
    }

    func test_testerExtension() {
        let state = true
        let tester = TestAction
            .tester(withState: true)
        XCTAssertEqual(state, tester.state)
    }

}

fileprivate enum TestAction: WorkflowAction {
    case toggleTapped
    case exitTapped

    typealias WorkflowType = TestWorkflow

    func apply(toState state: inout Bool) -> TestWorkflow.Output? {
        switch self {
        case .toggleTapped:
            state = !state
            return nil
        case .exitTapped:
            return .finished
        }
    }
}

fileprivate struct TestWorkflow: Workflow {

    typealias State = Bool

    enum Output {
        case finished
    }

    func makeInitialState() -> Bool {
        return true
    }

    func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout Bool) {

    }

    func render(state: Bool, context: RenderContext<TestWorkflow>) -> Void {
        return ()
    }

}
