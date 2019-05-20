import XCTest
@testable import Workflow

import ReactiveSwift
import Result


final class ConcurrencyTests: XCTestCase {

    // Applying an action from a sink must synchronously update the rendering.
    func test_sinkRenderLoopIsSynchronous() {
        let host = WorkflowHost(workflow: TestWorkflow())

        let expectation = XCTestExpectation()
        var first = true
        var observedScreen: TestWorkflow.TestScreen? = nil

        let disposable = host.rendering.signal.observeValues { rendering in
            if first {
                expectation.fulfill()
                first = false
            }
            observedScreen = rendering
        }

        let initialScreen = host.rendering.value
        XCTAssertEqual(0, initialScreen.count)
        initialScreen.update()

        // This update happens immediately as a new rendering is generated synchronously.
        XCTAssertEqual(1, host.rendering.value.count)

        wait(for: [expectation], timeout: 1.0)
        guard let screen = observedScreen else {
            XCTFail("Screen was not updated.")
            disposable?.dispose()
            return
        }
        XCTAssertEqual(1, screen.count)

        disposable?.dispose()
    }

    // Events emitted between `render` on a workflow and `enableEvents` are queued and will be delivered immediately when `enableEvents` is called.
    func test_queuedEvents() {
        let host = WorkflowHost(workflow: TestWorkflow())

        let expectation = XCTestExpectation()
        var first = true

        let disposable = host.rendering.signal.observeValues { rendering in
            if first {
                expectation.fulfill()
                first = false
                // Emit an event when the rendering is first received.
                rendering.update()
            }
        }

        let initialScreen = host.rendering.value
        XCTAssertEqual(0, initialScreen.count)

        // Updating the screen will cause two events - the `update` here, and the update caused by the first time the rendering changes.
        initialScreen.update()

        XCTAssertEqual(2, host.rendering.value.count)

        wait(for: [expectation], timeout: 1.0)

        disposable?.dispose()
    }

    // When events are queued, the debug info must be received in the order the events were processed.
    // This is to validate that `enableEvents` is tail recursive when handled by the WorkflowHost.
    func test_debugEventsAreOrdered() {
        final class Debugger: WorkflowDebugger {
            var snapshots: [WorkflowHierarchyDebugSnapshot] = []

            func didEnterInitialState(snapshot: WorkflowHierarchyDebugSnapshot) {
                // nop
            }

            func didUpdate(snapshot: WorkflowHierarchyDebugSnapshot, updateInfo: WorkflowUpdateDebugInfo) {
                snapshots.append(snapshot)
            }
        }

        let debugger = Debugger()
        let host = WorkflowHost(workflow: TestWorkflow(), debugger: debugger)

        var first = true
        let disposable = host.rendering.signal.observeValues { rendering in
            if first {
                first = false
                rendering.update()
            }
        }

        let initialScreen = host.rendering.value
        initialScreen.update()

        XCTAssertEqual(2, debugger.snapshots.count)
        XCTAssertEqual("1", debugger.snapshots[0].stateDescription)
        XCTAssertEqual("2", debugger.snapshots[1].stateDescription)

        disposable?.dispose()
    }

    // Signals are subscribed on a different scheduler than the UI scheduler,
    // which means that if they fire immediately, the action will be received after
    // `render` has completed.
    func test_subscriptionsAreAsync() {
        let signal = TestSignal()
        let host = WorkflowHost(
            workflow: TestWorkflow(
                running: .signal,
                signal: signal))

        let expectation = XCTestExpectation()
        let disposable = host.rendering.signal.observeValues { rendering in
            expectation.fulfill()
        }

        let screen = host.rendering.value

        XCTAssertEqual(0, screen.count)

        signal.send(value: 1)

        XCTAssertEqual(0, host.rendering.value.count)

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(1, host.rendering.value.count)

        disposable?.dispose()
    }

    // Workers are subscribed on a different scheduler than the UI scheduler,
    // which means that if they fire immediately, the action will be received after
    // `render` has completed.
    func test_workersAreAsync() {
        let host = WorkflowHost(
            workflow: TestWorkflow(
                running: .worker))

        let expectation = XCTestExpectation()
        let disposable = host.rendering.signal.observeValues { rendering in
            expectation.fulfill()
        }

        XCTAssertEqual(0, host.rendering.value.count)

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(1, host.rendering.value.count)

        disposable?.dispose()
    }

    fileprivate class TestSignal {
        let (signal, observer) = Signal<Int, NoError>.pipe()
        var sent: Bool = false

        func send(value: Int) {
            if !sent {
                observer.send(value: value)
                sent = true
            }
        }
    }

    fileprivate struct TestWorkflow: Workflow {

        init(running: Running = .idle, signal: TestSignal = TestSignal()) {
            self.running = running
            self.signal = signal
        }

        var running: Running
        enum Running {
            case idle
            case signal
            case worker
        }
        var signal: TestSignal

        struct State: CustomStringConvertible {
            var count: Int
            var running: Running
            var signal: TestSignal

            var description: String {
                return "\(count)"
            }
        }

        func makeInitialState() -> ConcurrencyTests.TestWorkflow.State {
            return State(count: 0, running: self.running, signal: self.signal)
        }

        func workflowDidChange(from previousWorkflow: ConcurrencyTests.TestWorkflow, state: inout ConcurrencyTests.TestWorkflow.State) {
        }

        enum Action: WorkflowAction {
            typealias WorkflowType = TestWorkflow

            case update

            func apply(toState state: inout ConcurrencyTests.TestWorkflow.State) -> ConcurrencyTests.TestWorkflow.Output? {
                switch self {
                case .update:
                    state.count += 1
                    return nil
                }
            }
        }

        struct TestScreen {
            var count: Int
            var update: () -> Void
        }

        typealias Rendering = TestScreen

        func render(state: ConcurrencyTests.TestWorkflow.State, context: RenderContext<ConcurrencyTests.TestWorkflow>) -> ConcurrencyTests.TestWorkflow.TestScreen {

            switch state.running {
            case .idle:
                break
            case .signal:
                context.subscribe(signal: signal.signal.map({ _ -> Action in
                    return .update
                }))

            case .worker:
                context.awaitResult(for: TestWorker())
            }

            let sink = context.makeSink(of: Action.self)

            return TestScreen(
                count: state.count,
                update: { sink.send(.update) })
        }

        struct TestWorker: Worker {
            typealias Output = TestWorkflow.Action

            func run() -> SignalProducer<ConcurrencyTests.TestWorkflow.Action, NoError> {
                return SignalProducer(value: .update)
            }

            func isEquivalent(to otherWorker: ConcurrencyTests.TestWorkflow.TestWorker) -> Bool {
                return true
            }
        }
    }
}
