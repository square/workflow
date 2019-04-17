import XCTest
@testable import Workflow
import ReactiveSwift
import Result

public class AnyWorkflowTests: XCTestCase {


    func testRendersWrappedWorkflow() {

        let workflow = AnyWorkflow(SimpleWorkflow(string: "asdf"))
        let node = WorkflowNode(workflow: PassthroughWorkflow(child: workflow))

        XCTAssertEqual(node.render(), "fdsa")
    }

    func testMapRendering() {

        let workflow = SimpleWorkflow(string: "asdf")
            .mapRendering { string -> String in
                return string + "dsa"
            }
        let node = WorkflowNode(workflow: PassthroughWorkflow(child: workflow))

        XCTAssertEqual(node.render(), "fdsadsa")
    }

}

/// Has no state or output, simply renders a reversed string
fileprivate struct PassthroughWorkflow<Rendering>: Workflow {
    var child: AnyWorkflow<Rendering, Never>
}

extension PassthroughWorkflow {

    struct State {}

    func makeInitialState(context: inout SideEffectContext<PassthroughWorkflow>) -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: PassthroughWorkflow<Rendering>, state: inout State, context: inout SideEffectContext<PassthroughWorkflow>) {

    }

    func compose(state: State, context: WorkflowContext<PassthroughWorkflow<Rendering>>) -> Rendering {
        return child.rendered(with: context)
    }

}




/// Has no state or output, simply renders a reversed string
fileprivate struct SimpleWorkflow: Workflow {
    var string: String
}

extension SimpleWorkflow {

    struct State {}

    func makeInitialState(context: inout SideEffectContext<SimpleWorkflow>) -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: SimpleWorkflow, state: inout State, context: inout SideEffectContext<SimpleWorkflow>) {

    }

    func compose(state: State, context: WorkflowContext<SimpleWorkflow>) -> String {
        return String(string.reversed())
    }

}
