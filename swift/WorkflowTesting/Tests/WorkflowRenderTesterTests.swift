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

    func test_workers() {
        let renderTester = TestWorkflow(initialText: "initial")
            .renderTester(
                initialState: TestWorkflow.State(
                    text: "otherText",
                    substate: .waiting))

        let expectedWorker = ExpectedWorker(worker: TestWorkflow.TestWorker(text: "otherText"))

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

        let expectedWorker = ExpectedWorker(worker: TestWorkflow.TestWorker(text: "otherText"), output: .success)
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
        ParentWorkflow(initialText: "hello")
            .renderTester()
            .render(
                with: RenderExpectations<ParentWorkflow>(),
                assertions: { rendering in
                    XCTAssertEqual("olleh", rendering)
                })
    }
}


private struct TestWorkflow: Workflow {
    /// Input
    var initialText: String

    /// Output
    enum Output {
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
        // TODO: Add a behavior when it changes to validate.
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


extension TestWorkflow {
    struct TestWorker: Worker {
        var text: String

        enum Output {
            case success
            case failure
        }

        func run() -> SignalProducer<Output, NoError> {
            return SignalProducer(value: .success)
        }

        func isEquivalent(to otherWorker: TestWorkflow.TestWorker) -> Bool {
            return text == otherWorker.text
        }
    }
}


struct TestScreen {
    var text: String
    var tapped: () -> Void
}


fileprivate struct ParentWorkflow: Workflow {

    var initialText: String

    struct State {
        var text: String
    }

    func makeInitialState() -> ParentWorkflow.State {
        return State(text: initialText)
    }

    func workflowDidChange(from previousWorkflow: ParentWorkflow, state: inout ParentWorkflow.State) {
    }

    func render(state: ParentWorkflow.State, context: RenderContext<ParentWorkflow>) -> String {
        return ChildWorkflow(text: state.text).rendered(with: context)
    }
}


fileprivate struct ChildWorkflow: Workflow {
    var text: String

    struct State {
    }

    func makeInitialState() -> ChildWorkflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: ChildWorkflow, state: inout ChildWorkflow.State) {
    }

    func render(state: ChildWorkflow.State, context: RenderContext<ChildWorkflow>) -> String {
        return String(text.reversed())
    }
}
