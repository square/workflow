/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Workflow

/// A set of expectations for use with the `WorkflowRenderTester`. All of the expectations must be fulfilled
/// for a `render` test to pass.
public struct RenderExpectations<WorkflowType: Workflow> {
    var expectedState: ExpectedState<WorkflowType>?
    var expectedOutput: ExpectedOutput<WorkflowType>?
    var expectedWorkers: [ExpectedWorker]
    var expectedWorkflows: [ExpectedWorkflow]
    var expectedSideEffects: [ExpectedSideEffect]

    public init(
        expectedState: ExpectedState<WorkflowType>? = nil,
        expectedOutput: ExpectedOutput<WorkflowType>? = nil,
        expectedWorkers: [ExpectedWorker] = [],
        expectedWorkflows: [ExpectedWorkflow] = [],
        expectedSideEffects: [ExpectedSideEffect] = []
    ) {
        self.expectedState = expectedState
        self.expectedOutput = expectedOutput
        self.expectedWorkers = expectedWorkers
        self.expectedWorkflows = expectedWorkflows
        self.expectedSideEffects = expectedSideEffects
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
        self.init(output: output, isEquivalent: { expected, actual in
            expected == actual
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
        self.init(state: state, isEquivalent: { expected, actual in
            expected == actual
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
        guard let expectedWorker = worker as? WorkerType else {
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

public struct ExpectedSideEffect {
    let key: AnyHashable

    public init(key: AnyHashable) {
        self.key = key
    }
}

public struct ExpectedWorkflow {
    let workflowType: Any.Type
    let key: String
    let rendering: Any
    let output: Any?

    public init<WorkflowType: Workflow>(type: WorkflowType.Type, key: String = "", rendering: WorkflowType.Rendering, output: WorkflowType.Output? = nil) {
        self.workflowType = type
        self.key = key
        self.rendering = rendering
        self.output = output
    }
}
