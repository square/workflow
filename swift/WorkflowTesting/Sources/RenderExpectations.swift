import Workflow


/// A set of expectations for use with the `WorkflowRenderTester`. All of the expectations must be fulfilled
/// for a `render` test to pass.
public struct RenderExpectations<WorkflowType: Workflow> {
    var expectedState: ExpectedState<WorkflowType>?
    var expectedOutput: ExpectedOutput<WorkflowType>?
    var expectedWorkers: [ExpectedWorker]
    var expectedWorkflows: [ExpectedWorkflow]

    public init(
        expectedState: ExpectedState<WorkflowType>? = nil,
        expectedOutput: ExpectedOutput<WorkflowType>? = nil,
        expectedWorkers: [ExpectedWorker] = [],
        expectedWorkflows: [ExpectedWorkflow] = []) {

        self.expectedState = expectedState
        self.expectedOutput = expectedOutput
        self.expectedWorkers = expectedWorkers
        self.expectedWorkflows = expectedWorkflows
    }
}


public struct ExpectedOutput<WorkflowType: Workflow> {
    let output: WorkflowType.Output
    let isEquivalent: (WorkflowType.Output, WorkflowType.Output) -> Bool

    public init<Output>(output: Output, isEquivalent: @escaping (Output, Output) -> Bool) where Output == WorkflowType.Output {
        self.output = output
        self.isEquivalent = isEquivalent
    }

    public init<Output>(output: Output) where Output == WorkflowType.Output, Output: Equatable {
        self.init(output: output, isEquivalent: { (expected, actual) in
            return expected == actual
        })
    }
}


public struct ExpectedState<WorkflowType: Workflow> {
    let state: WorkflowType.State
    let isEquivalent: (WorkflowType.State, WorkflowType.State) -> Bool

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
    let worker: Any
    private let output: Any?

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


public struct ExpectedWorkflow {
    let workflowType: Any.Type
    let key: String
    let rendering: Any
    private let output: Any?

    public init<WorkflowType: Workflow>(type: WorkflowType.Type, key: String = "", rendering: WorkflowType.Rendering, output: WorkflowType.Output? = nil) {
        self.workflowType = type
        self.key = key
        self.rendering = rendering
        self.output = output
    }
}
