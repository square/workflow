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

#if DEBUG

    // WorkflowTesting only available in Debug mode.
//
    // `@testable import Workflow` will fail compilation in Release mode.
    @testable import Workflow

    import ReactiveSwift
    import class Workflow.Lifetime
    import XCTest

    extension Workflow {
        /// Returns a `RenderTester` with a specified initial state.
        public func renderTester(initialState: Self.State) -> RenderTester<Self> {
            return RenderTester(workflow: self, state: initialState)
        }

        /// Returns a `RenderTester` with an initial state provided by `self.makeInitialState()`
        public func renderTester() -> RenderTester<Self> {
            return renderTester(initialState: makeInitialState())
        }
    }

    /// Testing helper for validating the behavior of calls to `render`.
    ///
    /// Usage: Set up a set of `RenderExpectations` and then validate with a call to `render`.
    /// Side-effects may be performed against the rendering to validate the behavior of actions.
    ///
    /// There is also a convenience `render` method where each expectation
    /// is an individual parameter.
    ///
    /// Child workflows will always be rendered based upon their initial state.
    ///
    /// To directly test actions and their effects, use the `WorkflowActionTester`.
    ///
    /// ```
    /// workflow
    ///     .renderTester(initialState: TestWorkflow.State())
    ///     .render(
    ///         with: RenderExpectations(
    ///         expectedState: ExpectedState(state: TestWorkflow.State()),
    ///         expectedOutput: ExpectedOutput(output: TestWorkflow.Output.finished),
    ///         expectedWorkers: [
    ///             ExpectedWorker(
    ///                 worker: TestWorker(),
    ///                 output: TestWorker.Output.success),
    ///             ...,
    ///         ]
    ///         expectedWorkflows: [
    ///             ExpectedWorkflow(
    ///                 type: ChildWorkflow.self,
    ///                 key: "key",
    ///                 rendering: "rendering",
    ///                 output: ChildWorkflow.Output.success),
    ///             ...,
    ///         ]),
    ///         assertions: { rendering in
    ///             XCTAssertEqual("expected text on rendering", rendering.text)
    ///         }
    ///     .render(...) // continue testing. The state will be updated based on actions or outputs.
    /// ```
    ///
    /// Using the convenience API
    /// ```
    /// workflow
    ///     .renderTester(initialState: TestWorkflow.State())
    ///     .render(
    ///         expectedState: ExpectedState(state: TestWorkflow.State()),
    ///         expectedOutput: ExpectedOutput(output: TestWorkflow.Output.finished),
    ///         expectedWorkers: [
    ///             ExpectedWorker(
    ///                 worker: TestWorker(),
    ///                 output: TestWorker.Output.success),
    ///             ...,
    ///         ]
    ///         expectedWorkflows: [
    ///             ExpectedWorkflow(
    ///                 type: ChildWorkflow.self,
    ///                 key: "key",
    ///                 rendering: "rendering",
    ///                 output: ChildWorkflow.Output.success)
    ///             ...,
    ///         ],
    ///         assertions: { rendering in
    ///             XCTAssertEqual("expected text on rendering", rendering.text)
    ///         }
    ///     .render(...) // continue testing. The state will be updated based on actions or outputs.
    /// ```
    ///
    /// Validating the rendering only from the initial state provided by the workflow:
    /// ```
    /// workflow
    ///     .renderTester()
    ///     .render(
    ///         with: RenderExpectations(),
    ///         assertions: { rendering in
    ///             XCTAssertEqual("expected text on rendering", rendering.text)
    ///         }
    /// ```
    ///
    /// Validate the state was updated from a callback on the rendering:
    /// ```
    /// workflow
    ///     .renderTester()
    ///     .render(
    ///         with: RenderExpectations(
    ///         expectedState: ExpectedState(state: TestWorkflow.State(text: "updated")),
    ///         assertions: { rendering in
    ///             XCTAssertEqual("expected text on rendering", rendering.text)
    ///             rendering.updateText("updated")
    ///         }
    /// ```
    ///
    /// Validate an output was received from the workflow. The `action()` on the rendering will cause an action that will return an output.
    /// ```
    /// workflow
    ///     .renderTester()
    ///     .render(
    ///         with: RenderExpectations(
    ///         expectedState: ExpectedOutput(output: .success)
    ///         assertions: { rendering in
    ///             rendering.action()
    ///         }
    /// ```
    ///
    /// Validate a worker is running, and simulate the effect of its output:
    /// ```
    /// workflow
    ///     .renderTester(initialState: TestWorkflow.State(loadingState: .loading))
    ///     .render(
    ///         with: RenderExpectations(
    ///         expectedState: ExpectedState(state: TestWorkflow.State(loadingState: .idle)),
    ///         expectedWorkers: [
    ///             ExpectedWorker(
    ///                 worker: TestWorker(),
    ///                 output: TestWorker.Output.success),
    ///             ...,
    ///         ]),
    ///         assertions: {}
    /// ```
    ///
    /// Validate a child workflow is run, and simulate the effect of its output:
    /// ```
    /// workflow
    ///     .renderTester(initialState: TestWorkflow.State(loadingState: .loading))
    ///     .render(
    ///         with: RenderExpectations(
    ///         expectedState: ExpectedState(state: TestWorkflow.State(loadingState: .idle)),
    ///         expectedWorkflows: [
    ///             ExpectedWorkflow(
    ///                 type: ChildWorkflow.self,
    ///                 rendering: "rendering",
    ///                 output: ChildWorkflow.Output.success
    ///         ]),
    ///         assertions: {}
    /// ```
    public final class RenderTester<WorkflowType: Workflow> {
        private var workflow: WorkflowType
        private var state: WorkflowType.State

        init(workflow: WorkflowType, state: WorkflowType.State) {
            self.workflow = workflow
            self.state = state
        }

        /// Call `render` with a set of expectations. If the expectations have not been fulfilled, the test will fail.
        @discardableResult
        public func render(file: StaticString = #file, line: UInt = #line, with expectations: RenderExpectations<WorkflowType>, assertions: (WorkflowType.Rendering) -> Void) -> RenderTester<WorkflowType> {
            let testContext = RenderTestContext<WorkflowType>(
                state: state,
                expectations: expectations,
                file: file,
                line: line
            )

            let context = RenderContext.make(implementation: testContext)
            let rendering = workflow.render(state: testContext.state, context: context)

            assertions(rendering)
            testContext.assertExpectations()

            state = testContext.state

            return self
        }

        /// Convenience method for testing without creating an explicit RenderExpectation.
        @discardableResult
        public func render(
            file: StaticString = #file, line: UInt = #line,
            expectedState: ExpectedState<WorkflowType>? = nil,
            expectedOutput: ExpectedOutput<WorkflowType>? = nil,
            expectedWorkers: [ExpectedWorker] = [],
            expectedWorkflows: [ExpectedWorkflow] = [],
            expectedSideEffects: [ExpectedSideEffect] = [],
            assertions: (WorkflowType.Rendering) -> Void
        ) -> RenderTester<WorkflowType> {
            let expectations = RenderExpectations(
                expectedState: expectedState,
                expectedOutput: expectedOutput,
                expectedWorkers: expectedWorkers,
                expectedWorkflows: expectedWorkflows
            )

            return render(file: file, line: line, with: expectations, assertions: assertions)
        }

        /// Assert the internal state.
        @discardableResult
        public func assert(state assertions: (WorkflowType.State) -> Void) -> RenderTester<WorkflowType> {
            assertions(state)
            return self
        }
    }

    fileprivate final class RenderTestContext<T: Workflow>: RenderContextType {
        typealias WorkflowType = T

        private var (lifetime, token) = ReactiveSwift.Lifetime.make()

        var state: WorkflowType.State
        var expectations: RenderExpectations<WorkflowType>
        let file: StaticString
        let line: UInt

        init(state: WorkflowType.State, expectations: RenderExpectations<WorkflowType>, file: StaticString, line: UInt) {
            self.state = state
            self.expectations = expectations
            self.file = file
            self.line = line
        }

        func render<Child, Action>(workflow: Child, key: String, outputMap: @escaping (Child.Output) -> Action) -> Child.Rendering where Child: Workflow, Action: WorkflowAction, RenderTestContext<T>.WorkflowType == Action.WorkflowType {
            guard let workflowIndex = expectations.expectedWorkflows.firstIndex(where: { expectedWorkflow -> Bool in
                type(of: workflow) == expectedWorkflow.workflowType && key == expectedWorkflow.key
            }) else {
                XCTFail("Unexpected child workflow of type \(workflow.self)", file: file, line: line)
                fatalError()
            }

            let expectedWorkflow = expectations.expectedWorkflows.remove(at: workflowIndex)
            if let childOutput = expectedWorkflow.output as? Child.Output {
                apply(action: outputMap(childOutput))
            }
            return expectedWorkflow.rendering as! Child.Rendering
        }

        func makeSink<Action>(of actionType: Action.Type) -> Sink<Action> where Action: WorkflowAction, T == Action.WorkflowType {
            let (signal, observer) = Signal<AnyWorkflowAction<WorkflowType>, Never>.pipe()
            let sink = Sink<Action> { action in
                observer.send(value: AnyWorkflowAction(action))
            }

            signal
                .take(during: lifetime)
                .observeValues { [weak self] action in
                    self?.apply(action: action)
                }

            return sink
        }

        func awaitResult<W, Action>(for worker: W, outputMap: @escaping (W.Output) -> Action) where W: Worker, Action: WorkflowAction, RenderTestContext<T>.WorkflowType == Action.WorkflowType {
            guard let workerIndex = expectations.expectedWorkers.firstIndex(where: { (expectedWorker) -> Bool in
                expectedWorker.isEquivalent(to: worker)
            }) else {
                XCTFail("Unexpected worker during render \(worker)", file: file, line: line)
                return
            }

            let expectedWorker = expectations.expectedWorkers.remove(at: workerIndex)
            if let action = expectedWorker.outputAction(outputMap: outputMap) {
                apply(action: action)
            }
        }

        func runSideEffect(key: AnyHashable, action: (Lifetime) -> Void) {
            guard let sideEffectIndex = expectations.expectedSideEffects.firstIndex(where: { (expectedSideEffect) -> Bool in
                expectedSideEffect.key == key
            }) else {
                XCTFail("Unexpected side-effect during render \(key)", file: file, line: line)
                return
            }

            expectations.expectedSideEffects.remove(at: sideEffectIndex)
        }

        private func apply<Action>(action: Action) where Action: WorkflowAction, Action.WorkflowType == WorkflowType {
            let output = action.apply(toState: &state)
            switch (output, expectations.expectedOutput) {
            case (.none, .none):
                // No expected output, no output received.
                break

            case (.some, .none):
                XCTFail("Received an output, but expected no output.", file: file, line: line)

            case (.none, .some):
                XCTFail("Expected an output, but received none.", file: file, line: line)

            case let (.some(output), .some(expectedOutput)):
                XCTAssertTrue(expectedOutput.isEquivalent(output, expectedOutput.output), "expect output of \(expectedOutput.output) but received \(output)", file: file, line: line)
            }
            expectations.expectedOutput = nil
        }

        /// Validate the expectations were fulfilled, or fail if not.
        func assertExpectations() {
            if let expectedState = expectations.expectedState {
                XCTAssertTrue(expectedState.isEquivalent(expectedState.state, state), "State: \(state) was not equivalent to expected state: \(expectedState.state)", file: file, line: line)
            }

            if let outputExpectation = expectations.expectedOutput {
                XCTFail("Expected output of '\(outputExpectation.output)' but received none.", file: file, line: line)
            }

            if !expectations.expectedWorkers.isEmpty {
                for expectedWorker in expectations.expectedWorkers {
                    XCTFail("Expected worker \(expectedWorker.worker)", file: file, line: line)
                }
            }

            if !expectations.expectedWorkflows.isEmpty {
                for expectedWorkflow in expectations.expectedWorkflows {
                    XCTFail("Expected child workflow of type: \(expectedWorkflow.workflowType) key: \(expectedWorkflow.key)", file: file, line: line)
                }
            }

            if !expectations.expectedSideEffects.isEmpty {
                for expectedSideEffect in expectations.expectedSideEffects {
                    XCTFail("Expected side-effect with key: \(expectedSideEffect.key)", file: file, line: line)
                }
            }
        }
    }

#endif
