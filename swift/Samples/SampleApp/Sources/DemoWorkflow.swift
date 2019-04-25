import Workflow
import WorkflowUI
import ReactiveSwift
import Result


// MARK: Input and Output

struct DemoWorkflow: Workflow {
    var name: String

    typealias Output = Never
}


// MARK: State and Initialization

extension DemoWorkflow {

    struct State {
        var colorState: ColorState
        var loadingState: LoadingState

        enum ColorState {
            case red
            case green
            case blue
        }

        enum LoadingState {
            case idle(title: String)
            case loading
        }
    }

    func makeInitialState() -> DemoWorkflow.State {
        return State(
            colorState: .red,
            loadingState: .idle(title: "Not Loaded"))
    }

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension DemoWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = DemoWorkflow

        case titleButtonTapped
        case refreshButtonTapped
        case refreshComplete(String)
        case refreshError(Error)

        func apply(toState state: inout DemoWorkflow.State) -> DemoWorkflow.Output? {

            switch self {
            case .titleButtonTapped:
                switch state.colorState {
                case .red:
                    state.colorState = .green
                case .green:
                    state.colorState = .blue
                case .blue:
                    state.colorState = .red
                }
            case .refreshButtonTapped:
                state.loadingState = .loading
            case .refreshComplete(let message):
                state.loadingState = .idle(title: message)
            case .refreshError(let error):
                state.loadingState = .idle(title: error.localizedDescription)
            }
            return nil
        }
    }
}


// MARK: Workers


struct RefreshWorker: Worker {
    enum Output {
        case success(String)
        case error(Error)
    }

    func run() -> SignalProducer<RefreshWorker.Output, NoError> {
        return SignalProducer(value: .success("We did it!"))
            .delay(1.0, on: QueueScheduler.main)
    }

    func isEquivalent(to otherWorker: RefreshWorker) -> Bool {
        return true
    }
}


// MARK: Rendering

extension DemoWorkflow {
    typealias Rendering = DemoScreen

    func render(state: DemoWorkflow.State, context: RenderContext<DemoWorkflow>) -> Rendering {
        let color: UIColor
        switch state.colorState {
        case .red:
            color = .red
        case .green:
            color = .green
        case .blue:
            color = .blue
        }

        var title = "Hello, \(name)!"
        let refreshText: String
        let refreshEnabled: Bool

        switch state.loadingState {
        case .idle(title: let refreshTitle):
            refreshText = refreshTitle
            refreshEnabled = true

            title = ReversingWorkflow(text: title)
                .rendered(with: context)

        case .loading:
            refreshText = "Loading..."
            refreshEnabled = false

            context.awaitResult(for: RefreshWorker()) { output -> Action in
                switch output {
                case .success(let result):
                    return .refreshComplete(result)
                case .error(let error):
                    return .refreshError(error)
                }
            }
        }

        // Create a sink of our Action type so we can send actions back to the workflow.
        let sink = context.makeSink(of: Action.self)

        return DemoScreen(
            title: title,
            color: color,
            onTitleTap: {
                sink.send(.titleButtonTapped)
            },
            refreshText: refreshText,
            isRefreshEnabled: refreshEnabled,
            onRefreshTap: {
                sink.send(.refreshButtonTapped)
            })
    }
}
