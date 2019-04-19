import Workflow
import WorkflowUI


// MARK: Input and Output

struct WelcomeWorkflow: Workflow {
    enum Output {
        case login(name: String)
    }
}


// MARK: State and Initialization

extension WelcomeWorkflow {

    struct State {
        var name: String
    }

    func makeInitialState() -> WelcomeWorkflow.State {
        return State(name: "")
    }

    func workflowDidChange(from previousWorkflow: WelcomeWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension WelcomeWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = WelcomeWorkflow

        case nameChanged(String)
        case login

        func apply(toState state: inout WelcomeWorkflow.State) -> WelcomeWorkflow.Output? {

            switch self {
            case .nameChanged(let updatedName):
                state.name = updatedName
                return nil

            case .login:
                return .login(name: state.name)
            }
        }
    }
}


// MARK: Rendering

extension WelcomeWorkflow {
    typealias Rendering = WelcomeScreen

    func render(state: WelcomeWorkflow.State, context: RenderContext<WelcomeWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        return WelcomeScreen(
            name: state.name,
            onNameChanged: { updatedName in
                sink.send(.nameChanged(updatedName))
            },
            onLoginTapped: {
                sink.send(.login)
            })
    }
}
