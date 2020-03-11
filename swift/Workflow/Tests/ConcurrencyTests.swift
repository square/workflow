/*
 * Copyright 2019 Square Inc.
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
import XCTest
@testable import Workflow

import ReactiveSwift


final class ConcurrencyTests: XCTestCase {

    // Applying an action from a sink must synchronously update the rendering.
    func test_sinkRenderLoopIsSynchronous() {
        let host = WorkflowHost(workflow: TestWorkflow())

        let expectation = XCTestExpectation()
        var first = true
        var observedScreen: TestScreen? = nil

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

    // A `sink` is invalidated after a single action has been received. However, if the next `render` pass uses a sink
    // of the same type, actions sent to an old sink should be proxied through the new sink.
    // This allows for a UI that does not synchronously update to use the new sink.
    func test_old_sink_proxies_to_new_sink() {
        let host = WorkflowHost(workflow: TestWorkflow())

        // Capture the initial screen and corresponding closure that uses the original sink.
        let initialScreen = host.rendering.value
        XCTAssertEqual(0, initialScreen.count)

        // Send an action to the workflow. This invalidates this sink, but the next render pass declares a
        // sink of the same type.
        initialScreen.update()

        let secondScreen = host.rendering.value
        XCTAssertEqual(1, secondScreen.count)

        // Send an action from the original screen and sink. It should be proxied through the most recent sink.
        initialScreen.update()

        let thirdScreen = host.rendering.value
        XCTAssertEqual(2, thirdScreen.count)
    }

    // If a previous `sink` has been invalidated due to receiving an action, and a new sink of the same type
    // is not redeclared on the subsequent render pass, it should be considered invalid and not allowed to send actions.
    func test_invalidate_old_sink_if_not_redeclared() {
        let host = WorkflowHost(workflow: OneShotWorkflow())

        // Capture the initial screen and corresponding closure that uses the original sink.
        let initialScreen = host.rendering.value
        XCTAssertEqual(0, initialScreen.count)

        // Send an action to the workflow. This invalidates this sink, but the next render pass declares a
        // sink of the same type.
        initialScreen.update()

        let secondScreen = host.rendering.value
        XCTAssertEqual(1, secondScreen.count)

        // MANUAL TEST CASE: Uncomment to validate this fatal errors.
        // Calling `update` uses the original sink. This will fail with a fatalError as the sink was not redeclared.
        //initialScreen.update()

        // If the sink *was* still valid, this would be correct. However, it should just fail and be `1` still.
        //XCTAssertEqual(2, secondScreen.count)
        // Actual expected result, if we had not fatal errored.
        XCTAssertEqual(1, host.rendering.value.count)

        struct OneShotWorkflow: Workflow {
            typealias Output = Never
            struct State {
                var count: Int
            }

            func makeInitialState() -> State {
                return State(count: 0)
            }

            func workflowDidChange(from previousWorkflow: OneShotWorkflow, state: inout State) {
            }

            enum Action: WorkflowAction {
                typealias WorkflowType = OneShotWorkflow

                case updated

                func apply(toState state: inout State) -> Never? {
                    switch self {
                    case .updated:
                        state.count += 1
                        return nil
                    }
                }
            }

            typealias Rendering = TestScreen
            func render(state: State, context: RenderContext<OneShotWorkflow>) -> Rendering {
                let update: () -> Void
                if state.count == 0 {
                    let sink = context.makeSink(of: Action.self)
                    update = {
                        sink.send(.updated)
                    }
                } else {
                    update = {}
                }
                return TestScreen(count: state.count, update: update)
            }
        }
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
        withExtendedLifetime(host) {
            initialScreen.update()
        }

        XCTAssertEqual(2, debugger.snapshots.count)
        XCTAssertEqual("1", debugger.snapshots[0].stateDescription)
        XCTAssertEqual("2", debugger.snapshots[1].stateDescription)

        disposable?.dispose()
    }

    func test_childWorkflowsAreSynchronous() {

        let host = WorkflowHost(workflow: ParentWorkflow())

        let initialScreen = host.rendering.value
        XCTAssertEqual(0, initialScreen.count)
        initialScreen.update()

        // This update happens immediately as a new rendering is generated synchronously.
        // Both the child updates from the action (incrementing state by 1) as well as the
        // parent from the output (incrementing its state by 10)
        XCTAssertEqual(11, host.rendering.value.count)

        struct ParentWorkflow: Workflow {
            struct State {
                var count: Int
            }

            func makeInitialState() -> State {
                return State(count: 0)
            }

            func workflowDidChange(from previousWorkflow: ParentWorkflow, state: inout State) {
            }

            enum Action: WorkflowAction {
                typealias WorkflowType = ParentWorkflow

                case update

                func apply(toState state: inout State) -> Output? {
                    switch self {
                    case .update:
                        state.count += 10
                        return nil
                    }
                }
            }

            typealias Rendering = TestScreen

            func render(state: State, context: RenderContext<ParentWorkflow>) -> Rendering {
                var childScreen = TestWorkflow(running: .idle, signal: TestSignal())
                    .mapOutput({ output -> Action in
                        switch output {
                        case .emit:
                            return .update
                        }
                    })
                    .rendered(with: context)

                childScreen.count += state.count
                return childScreen
            }
        }

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

    // Since event pipes are reused for the same type, validate that the `AnyWorkflowAction`
    // defined event pipes still sends through the correct action.
    // Because they are just backed by type, not the actual action, they send the actions appropriately.
    // (Thus, there is a single backing `TypedSink` for `AnyWorkflowAction`, but the correct action is applied.
    func test_multipleAnyWorkflowAction_sinksDontOverrideEachOther() {

        let host = WorkflowHost(workflow: AnyActionWorkflow())

        let initialScreen = host.rendering.value
        XCTAssertEqual(0, initialScreen.count)

        // Update using the first action.
        initialScreen.updateFirst()

        let secondScreen = host.rendering.value
        XCTAssertEqual(1, secondScreen.count)

        // Update using the second action.
        secondScreen.updateSecond()
        XCTAssertEqual(11, host.rendering.value.count)

        struct AnyActionWorkflow: Workflow {
            enum Output {
                case emit
            }

            struct State {
                var count: Int
            }

            func makeInitialState() -> State {
                return State(count: 0)
            }

            func workflowDidChange(from previousWorkflow: AnyActionWorkflow, state: inout State) {

            }

            enum FirstAction: WorkflowAction {

                typealias WorkflowType = AnyActionWorkflow
                case update

                func apply(toState state: inout State) -> Output? {
                    switch self {
                    case .update:
                        state.count += 1
                    }
                    return nil
                }
            }

            enum SecondAction: WorkflowAction {

                typealias WorkflowType = AnyActionWorkflow
                case update

                func apply(toState state: inout State) -> Output? {
                    switch self {
                    case .update:
                        state.count += 10
                    }
                    return nil
                }
            }

            struct TestScreen {
                var count: Int
                var updateFirst: () -> Void
                var updateSecond: () -> Void
            }
            typealias Rendering = TestScreen

            func render(state: State, context: RenderContext<AnyActionWorkflow>) -> Rendering {

                let firstSink = context
                    .makeSink(
                        of: AnyWorkflowAction.self)
                    .contraMap { (action: FirstAction) -> AnyWorkflowAction<AnyActionWorkflow> in
                        AnyWorkflowAction(action)
                    }

                let secondSink = context
                    .makeSink(
                        of: AnyWorkflowAction.self)
                    .contraMap { (action: SecondAction) -> AnyWorkflowAction<AnyActionWorkflow> in
                        AnyWorkflowAction(action)
                    }

                return TestScreen(
                    count: state.count,
                    updateFirst: {
                        firstSink.send(.update)
                    },
                    updateSecond: {
                        secondSink.send(.update)
                    })
            }
        }

    }

    /// Since event pipes are allowed to be reused, and shared for the same backing action type
    /// validate that they are also only reused for the same source type - ie: a sink for an
    /// action should not use the same event pipe as a worker that maps to the same action type.
    /// This will likely need to be a test that will be "correct" when it fatal errors
    /// since the behavior would be reusing the wrong event pipe for an old sink that was not
    /// redeclared.
    func test_eventPipesAreOnlyReusedForSameSource() {
        let host = WorkflowHost(workflow: SourceDifferentiatingWorkflow(step: .first))

        //let initialScreen = host.rendering.value
        XCTAssertEqual(0, host.rendering.value.count)

        // Update to the second "step", which will cause a render update, with the sink not being declared.
        host.update(workflow: SourceDifferentiatingWorkflow(step: .second))
        // The state should be the same, even though it rendered again.
        XCTAssertEqual(0, host.rendering.value.count)

        // MANUAL TEST CASE
        // This will fail, as the sink held by `initialScreen` has not be redeclared, even though the backing action is the same for the worker.
        // Uncomment to validate this test fails with a fatal error.
        //initialScreen.update()
        XCTAssertEqual(0, host.rendering.value.count)

        struct SourceDifferentiatingWorkflow: Workflow {

            var step: Step
            enum Step {
                case first
                case second
            }

            struct State {
                var count: Int
            }

            func makeInitialState() -> State {
                return State(count: 0)
            }

            func workflowDidChange(from previousWorkflow: SourceDifferentiatingWorkflow, state: inout State) {
            }

            enum Action: WorkflowAction {
                typealias WorkflowType = SourceDifferentiatingWorkflow

                case update

                func apply(toState state: inout State) -> Never? {
                    switch self {
                    case .update:
                        state.count += 1
                        return nil
                    }
                }
            }

            struct DelayWorker: Worker {
                typealias Output = Action

                func run() -> SignalProducer<Output, Never> {
                    return SignalProducer(value: .update).delay(0.1, on: QueueScheduler.main)
                }

                func isEquivalent(to otherWorker: DelayWorker) -> Bool {
                    return true
                }
            }

            typealias Rendering = TestScreen

            func render(state: State, context: RenderContext<SourceDifferentiatingWorkflow>) -> Rendering {
                let update: () -> Void
                switch step {

                case .first:
                    let sink = context.makeSink(of: Action.self)
                    update = { sink.send(.update) }

                case .second:
                    update = {}
                }

                DelayWorker().running(with: context)

                return TestScreen(count: state.count, update: update)
            }
        }
    }

    // MARK - Test Types

    fileprivate class TestSignal {
        let (signal, observer) = Signal<Int, Never>.pipe()
        var sent: Bool = false

        func send(value: Int) {
            if !sent {
                observer.send(value: value)
                sent = true
            }
        }
    }


    fileprivate struct TestScreen {
        var count: Int
        var update: () -> Void
    }


    fileprivate struct TestWorkflow: Workflow {

        enum Output {
            case emit
        }

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

        func makeInitialState() -> State {
            return State(count: 0, running: self.running, signal: self.signal)
        }

        func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout State) {
        }

        enum Action: WorkflowAction {
            typealias WorkflowType = TestWorkflow

            case update

            func apply(toState state: inout State) -> Output? {
                switch self {
                case .update:
                    state.count += 1
                    return .emit
                }
            }
        }

        typealias Rendering = TestScreen

        func render(state: State, context: RenderContext<TestWorkflow>) -> Rendering {

            switch state.running {
            case .idle:
                break
            case .signal:
                context.subscribe(signal: signal.signal.map({ _ -> Action in
                    return .update
                }))

            case .worker:
                TestWorker().running(with: context)
            }

            let sink = context.makeSink(of: Action.self)

            return TestScreen(
                count: state.count,
                update: { sink.send(.update) })
        }

        struct TestWorker: Worker {
            typealias Output = TestWorkflow.Action

            func run() -> SignalProducer<Output, Never> {
                return SignalProducer(value: .update)
            }

            func isEquivalent(to otherWorker: TestWorker) -> Bool {
                return true
            }
        }
    }
}
