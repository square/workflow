import XCTest
import ReactiveSwift
import Result
@testable import Workflow


extension Workflow {

    /// Returns a `RenderTester` with a specified initial state.
    public func renderTester(initialState: Self.State) -> RenderTester<Self> {
        return RenderTester(workflow: self, state: initialState)
    }

    /// Returns a `RenderTester` with an initial state provided by `self.makeInitialState()`
    public func renderTester() -> RenderTester<Self> {
        return self.renderTester(initialState: self.makeInitialState())
    }

}


/// Testing helper for validating the behavior of calls to `render`.
///
/// Usage: Set up a set of `RenderExpectations` and then validate with a call to `render`.
/// Side-effects may be performed against the rendering to validate the behavior of actions.
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
///         expectedWorkers: [
///             ExpectedWorker(
///                 worker: TestWorker(),
///                 output: TestWorker.Output.success),
///             ...,
///         ]),
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
public final class RenderTester<WorkflowType: Workflow> {
    private var workflow: WorkflowType
    private var state: WorkflowType.State

    init(workflow: WorkflowType, state: WorkflowType.State) {
        self.workflow = workflow
        self.state = state
    }

    /// Call `render` with a set of expectations. If the expectations have not been fulfilled, the test will fail.
    @discardableResult
    public func render(with expectations: RenderExpectations<WorkflowType>, assertions: (WorkflowType.Rendering) -> Void, file: StaticString = #file, line: UInt = #line) -> RenderTester<WorkflowType> {

        let testContext = RenderTestContext<WorkflowType>(
            state: state,
            expectations: expectations,
            file: file,
            line: line)

        let context = RenderContext.make(implementation: testContext)
        let rendering = workflow.render(state: testContext.state, context: context)

        assertions(rendering)
        testContext.assertExpectations()

        self.state = testContext.state

        return self
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

    private var (lifetime, token) = Lifetime.make()

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

    func render<Child, Action>(workflow: Child, key: String, outputMap: @escaping (Child.Output) -> Action) -> Child.Rendering where Child : Workflow, Action : WorkflowAction, RenderTestContext<T>.WorkflowType == Action.WorkflowType {
        let testContext = RenderTestContext<Child>(
            state: workflow.makeInitialState(),
            expectations: RenderExpectations<Child>(),
            file: file,
            line: line)
        let context = RenderContext.make(implementation: testContext)
        return workflow.render(state: testContext.state, context: context)
    }

    func makeSink<Action>(of actionType: Action.Type) -> Sink<Action> where Action : WorkflowAction, T == Action.WorkflowType {
        let (signal, observer) = Signal<AnyWorkflowAction<WorkflowType>, NoError>.pipe()
        let sink = Sink<Action> { action in
            observer.send(value: AnyWorkflowAction(action))
        }
        subscribe(signal: signal)
        return sink
    }

    func subscribe<Action>(signal: Signal<Action, NoError>) where Action : WorkflowAction, RenderTestContext<T>.WorkflowType == Action.WorkflowType {
        signal
            .take(during: lifetime)
            .observeValues { [weak self] action in
                self?.apply(action: action)
            }
    }

    func awaitResult<W, Action>(for worker: W, outputMap: @escaping (W.Output) -> Action) where W : Worker, Action : WorkflowAction, RenderTestContext<T>.WorkflowType == Action.WorkflowType {

        guard let workerIndex = expectations.expectedWorkers.firstIndex(where: { (expectedWorker) -> Bool in
            return expectedWorker.isEquivalent(to: worker)
        }) else {
            XCTFail("Unexpected worker during render \(worker)", file: file, line: line)
            return
        }

        let expectedWorker = expectations.expectedWorkers.remove(at: workerIndex)
        if let action = expectedWorker.outputAction(outputMap: outputMap) {
            apply(action: action)
        }
    }

    private func apply<Action>(action: Action) where Action: WorkflowAction, Action.WorkflowType == WorkflowType {
        let _ = action.apply(toState: &self.state)
    }

    /// Validate the expectations were fulfilled, or fail if not.
    func assertExpectations() {
        if let expectedState = expectations.expectedState {
            XCTAssertTrue(expectedState.isEquivalent(expectedState.state, self.state), "State: \(self.state) was not equivalent to expected state: \(expectedState.state)", file: file, line: line)
        }

        if expectations.expectedWorkers.count != 0 {
            for expectedWorker in expectations.expectedWorkers {
                XCTFail("Expected worker \(expectedWorker.worker)", file: file, line: line)
            }
        }

    }
}
