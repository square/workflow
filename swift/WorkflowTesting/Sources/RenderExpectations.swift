import Workflow


/// A set of expectations for use with the `WorkflowRenderTester`. All of the expectations must be fulfilled
/// for a `render` test to pass.
public struct RenderExpectations<WorkflowType: Workflow> {
    var expectedState: ExpectedState<WorkflowType>?
    var expectedWorkers: [ExpectedWorker]

    public init(
        expectedState: ExpectedState<WorkflowType>? = nil,
        expectedWorkers: [ExpectedWorker] = []) {

        self.expectedState = expectedState
        self.expectedWorkers = expectedWorkers
    }
}


public struct ExpectedState<WorkflowType: Workflow> {
    var state: WorkflowType.State
    var isEquivalent: (WorkflowType.State, WorkflowType.State) -> Bool

    /// Create a new expected state from a state with an equivalence block. `isEquivalent` will be
    /// called to validate that the expected state matches the actual state after a render pass.
    public init<State>(state: State, isEquivalent: @escaping (State, State) -> Bool) where State == WorkflowType.State {
        self.state = state
        self.isEquivalent = isEquivalent
    }

    public init<State>(state: State) where WorkflowType.State == State, State: Equatable {
        self.init(state: state, isEquivalent: { (expected, actual) in
            return expected == actual
        })
    }
}


public struct ExpectedWorker {
    var worker: Any
    private var output: Any?

    /// Create a new expected worker with an optional output. If `output` is not nil, it will be emitted
    /// when this worker is declared in the render pass.
    public init<WorkerType: Worker>(worker: WorkerType, output: WorkerType.Output? = nil) {
        self.worker = worker
        self.output = output
    }

    func isEquivalent<WorkerType: Worker>(to actual: WorkerType) -> Bool {
        guard let expectedWorker = self.worker as? WorkerType else {
            return false
        }

        return expectedWorker.isEquivalent(to: actual)
    }

    func outputAction<Output, ActionType>(outputMap: (Output) -> ActionType) -> ActionType? where ActionType: WorkflowAction {
        guard let output = output as? Output else {
            return nil
        }

        return outputMap(output)
    }
}
