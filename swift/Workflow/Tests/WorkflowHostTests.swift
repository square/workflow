import XCTest
import Workflow


final class WorkflowHostTests: XCTestCase {

    func test_updatedInputCausesRenderPass() {
        let host = WorkflowHost(workflow: TestWorkflow(step: .first))

        XCTAssertEqual(1, host.rendering.value)

        host.update(workflow: TestWorkflow(step: .second))

        XCTAssertEqual(2, host.rendering.value)
    }

    fileprivate struct TestWorkflow: Workflow {
        var step: Step
        enum Step {
            case first
            case second
        }

        struct State {}
        func makeInitialState() -> State {
            return State()
        }

        func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout State) {
        }

        typealias Rendering = Int

        func render(state: State, context: RenderContext<TestWorkflow>) -> Rendering {
            switch self.step {
            case .first:
                return 1
            case .second:
                return 2
            }
        }
    }
}
