import XCTest
import ReactiveSwift
import Result
import Workflow
import WorkflowTesting


final class WorkflowRenderTesterTests: XCTestCase {
    func test_assertState() {
        let renderTester = TestWorkflow(initialText: "initial").renderTester()
        var testedAssertion = false

        renderTester.assert { state in
            XCTAssertEqual("initial", state.text)
            XCTAssertEqual(.idle, state.substate)
            testedAssertion = true
        }
        XCTAssertTrue(testedAssertion)
    }

    func test_render() {
        let renderTester = TestWorkflow(initialText: "initial").renderTester()
        var testedAssertion = false

        renderTester.render(
            with: RenderExpectations(
                expectedState: ExpectedState(
                    state: TestWorkflow.State(
                        text: "initial",
                        substate: .idle))),
            assertions: { screen in
                XCTAssertEqual("initial", screen.text)
                testedAssertion = true
        })
        XCTAssertTrue(testedAssertion)
    }

    func test_action() {
        let renderTester = TestWorkflow(initialText: "initial").renderTester()

        renderTester.render(
            with: RenderExpectations(
                expectedState: ExpectedState(
                    state: TestWorkflow.State(
                        text: "initial",
                        substate: .waiting))),
            assertions: { screen in
                XCTAssertEqual("initial", screen.text)
                screen.tapped()
        })
    }

    func test_output() {
        OutputWorkflow()
            .renderTester()
            .render(
                with: RenderExpectations(
                    expectedOutput: ExpectedOutput(output: .success)),
                assertions: { rendering in
                    rendering.tapped()
            })
    }

    func test_workers() {
        let renderTester = TestWorkflow(initialText: "initial")
            .renderTester(
                initialState: TestWorkflow.State(
                    text: "otherText",
                    substate: .waiting))

        let expectedWorker = ExpectedWorker(worker: TestWorker(text: "otherText"))

        renderTester.render(
            with: RenderExpectations(
                expectedState: nil,
                expectedWorkers: [expectedWorker]),
            assertions: { screen in
                XCTAssertEqual("otherText", screen.text)
            })
    }

    func test_workerOutput() {
        let renderTester = TestWorkflow(initialText: "initial")
            .renderTester(initialState: TestWorkflow.State(
                text: "otherText",
                substate: .waiting))

        let expectedWorker = ExpectedWorker(worker: TestWorker(text: "otherText"), output: .success)
        let expectedState = ExpectedState<TestWorkflow>(state: TestWorkflow.State(text: "otherText", substate: .idle))

        renderTester.render(
            with: RenderExpectations(
                expectedState: expectedState,
                expectedWorkers: [expectedWorker]),
            assertions: { screen in
                XCTAssertEqual("otherText", screen.text)
            })
    }

    func test_childWorkflow() {
        // Test the child independently from the parent.
        ChildWorkflow(text: "hello")
            .renderTester()
            .render(with: RenderExpectations<ChildWorkflow>(
                expectedOutput: ExpectedOutput(output: .success),
                expectedWorkers: [
                    ExpectedWorker(
                        worker: TestWorker(text: "hello"),
                        output: .success)
                    ]),
                    assertions: { rendering in
                        XCTAssertEqual("olleh", rendering)
                })

        // Test the parent simulating the behavior of the child. The worker would run, but because the child is simulated, does not run.
        ParentWorkflow(initialText: "hello")
            .renderTester()
            .render(
                with: RenderExpectations<ParentWorkflow>(expectedWorkflows: [
                    ExpectedWorkflow(
                        type: ChildWorkflow.self,
                        rendering: "olleh",
                        output: nil)
                    ]),
                assertions: { rendering in
                    XCTAssertEqual("olleh", rendering)
                })
    }

    func test_implict_expectations() {
        TestWorkflow(initialText: "hello")
            .renderTester()
            .render(
                expectedState: ExpectedState<TestWorkflow>(
                    state: TestWorkflow.State(
                        text: "hello",
                        substate: .idle)),
                expectedOutput: nil,
                expectedWorkers: [],
                expectedWorkflows: [],
                assertions: { rendering in
                    XCTAssertEqual("hello", rendering.text)
            })
    }
}


private struct TestWorkflow: Workflow {
    /// Input
    var initialText: String

    /// Output
    enum Output: Equatable {
        case first
    }

    struct State: Equatable {
        var text: String
        var substate: Substate
        enum Substate: Equatable {
            case idle
            case waiting
        }
    }

    func makeInitialState() -> State {
        return State(text: initialText, substate: .idle)
    }

    func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout TestWorkflow.State) {
    }

    func render(state: State, context: RenderContext<TestWorkflow>) -> TestScreen {
        let sink = context.makeSink(of: Action.self)

        switch state.substate {
        case .idle:
            break
        case .waiting:
            context.awaitResult(for: TestWorker(text: state.text)) { output -> Action in
                return .asyncSuccess
            }
        }

        return TestScreen(
            text: state.text,
            tapped: {
                sink.send(.tapped)
            })
    }
}

extension TestWorkflow {
    enum Action: WorkflowAction, Equatable {
        typealias WorkflowType = TestWorkflow

        case tapped
        case asyncSuccess

        func apply(toState state: inout TestWorkflow.State) -> TestWorkflow.Output? {

            switch self {
            case .tapped:
                state.substate = .waiting

            case .asyncSuccess:
                state.substate = .idle
            }
            return nil
        }
    }
}


fileprivate struct OutputWorkflow: Workflow {
    enum Output {
        case success
        case failure
    }

    struct State {}

    func makeInitialState() -> OutputWorkflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: OutputWorkflow, state: inout OutputWorkflow.State) {
    }

    enum Action: WorkflowAction {
        typealias WorkflowType = OutputWorkflow

        case emit

        func apply(toState state: inout OutputWorkflow.State) -> OutputWorkflow.Output? {
            switch self {
            case .emit:
                return .success
            }
        }
    }

    typealias Rendering = TestScreen

    func render(state: State, context: RenderContext<OutputWorkflow>) -> TestScreen {
        let sink = context.makeSink(of: Action.self)

        return TestScreen(text: "value", tapped: {
            sink.send(.emit)
        })
    }
}


fileprivate struct TestWorker: Worker {
    var text: String

    enum Output {
        case success
        case failure
    }

    func run() -> SignalProducer<Output, NoError> {
        return SignalProducer(value: .success)
    }

    func isEquivalent(to otherWorker: TestWorker) -> Bool {
        return text == otherWorker.text
    }
}


fileprivate struct TestScreen {
    var text: String
    var tapped: () -> Void
}


fileprivate struct ParentWorkflow: Workflow {
    typealias Output = Never

    var initialText: String

    struct State {
        var text: String
    }

    func makeInitialState() -> ParentWorkflow.State {
        return State(text: initialText)
    }

    func workflowDidChange(from previousWorkflow: ParentWorkflow, state: inout ParentWorkflow.State) {
    }

    enum Action: WorkflowAction {
        typealias WorkflowType = ParentWorkflow

        case childSuccess
        case childFailure

        func apply(toState state: inout ParentWorkflow.State) -> Never? {
            switch self {

            case .childSuccess:
                state.text = String(state.text.reversed())

            case .childFailure:
                state.text = "Failed"
            }

            return nil
        }
    }

    func render(state: ParentWorkflow.State, context: RenderContext<ParentWorkflow>) -> String {
        return ChildWorkflow(text: state.text)
            .mapOutput({ output -> Action in
                switch output {
                case .success:
                    return .childSuccess
                case .failure:
                    return .childFailure
                }
            })
            .rendered(with: context)
    }
}


fileprivate struct ChildWorkflow: Workflow {
    enum Output: Equatable {
        case success
        case failure
    }

    var text: String

    struct State {
    }

    func makeInitialState() -> ChildWorkflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: ChildWorkflow, state: inout ChildWorkflow.State) {
    }

    func render(state: ChildWorkflow.State, context: RenderContext<ChildWorkflow>) -> String {
        context.awaitResult(
            for: TestWorker(text: text),
            onOutput: { (output, state) -> Output in
                return .success
            })

        return String(text.reversed())
    }
}
