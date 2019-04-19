import Workflow
import WorkflowUI


// MARK: Input and Output

/// This is a stateless workflow. It only used the properties sent from its parent to render a result.
struct ReversingWorkflow: Workflow {
    typealias Rendering = String
    typealias Output = Never

    var text: String
}


// MARK: State and Initialization

extension ReversingWorkflow {

    struct State {

    }

    func makeInitialState() -> ReversingWorkflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: ReversingWorkflow, state: inout State) {

    }
}


// MARK: Rendering

extension ReversingWorkflow {

    func render(state: ReversingWorkflow.State, context: RenderContext<ReversingWorkflow>) -> String {
        return String(text.reversed())
    }
}
