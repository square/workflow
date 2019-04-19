import Workflow
import WorkflowUI
import ReactiveSwift
import Result


// MARK: Input and Output

struct RootWorkflow: Workflow {
    typealias Output = Never
}


// MARK: State and Initialization

extension RootWorkflow {

    enum State {
        case welcome
        case demo(name: String)
    }

    func makeInitialState() -> RootWorkflow.State {
        return .welcome
    }

    func workflowDidChange(from previousWorkflow: RootWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension RootWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = RootWorkflow

        case login(name: String)

        func apply(toState state: inout RootWorkflow.State) -> RootWorkflow.Output? {

            switch self {
            case .login(name: let name):
                state = .demo(name: name)
            }

            return nil
        }
    }
}


// MARK: Rendering

extension RootWorkflow {
    typealias Rendering = CrossFadeScreen

    func render(state: RootWorkflow.State, context: RenderContext<RootWorkflow>) -> Rendering {
        switch state {
        case .welcome:
            return CrossFadeScreen(
                base: WelcomeWorkflow()
                    .mapOutput({ output -> Action in
                        switch output {
                        case .login(name: let name):
                            return .login(name: name)
                        }
                    })
                    .rendered(with: context))

        case .demo(name: let name):
            return CrossFadeScreen(
                base: DemoWorkflow(name: name)
                    .rendered(with: context))
        }
    }
}
