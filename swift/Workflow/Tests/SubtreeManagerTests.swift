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

import ReactiveSwift
import XCTest
@testable import Workflow

final class SubtreeManagerTests: XCTestCase {
    func test_maintainsChildrenBetweenRenderPasses() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()
        XCTAssertTrue(manager.childWorkflows.isEmpty)

        _ = manager.render { context -> TestViewModel in
            context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.childWorkflows.count, 1)
        let child = manager.childWorkflows.values.first!

        _ = manager.render { context -> TestViewModel in
            context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.childWorkflows.count, 1)
        XCTAssertTrue(manager.childWorkflows.values.first === child)
    }

    func test_removesUnusedChildrenAfterRenderPasses() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()
        _ = manager.render { context -> TestViewModel in
            context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.childWorkflows.count, 1)

        _ = manager.render { context -> Void in
        }

        XCTAssertTrue(manager.childWorkflows.isEmpty)
    }

    func test_emitsChildEvents() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()

        var events: [AnyWorkflowAction<ParentWorkflow>] = []

        manager.onUpdate = {
            switch $0 {
            case let .update(event, _):
                events.append(event)
            default:
                break
            }
        }

        let viewModel = manager.render { context -> TestViewModel in
            context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }
        manager.enableEvents()

        viewModel.onTap()
        viewModel.onTap()

        RunLoop.current.run(until: Date().addingTimeInterval(0.1))

        XCTAssertEqual(events.count, 2)
    }

    func test_emitsChangeEvents() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()

        var changeCount = 0

        manager.onUpdate = { _ in
            changeCount += 1
        }

        let viewModel = manager.render { context -> TestViewModel in
            context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }
        manager.enableEvents()

        viewModel.onToggle()
        viewModel.onToggle()

        RunLoop.current.run(until: Date().addingTimeInterval(0.1))

        XCTAssertEqual(changeCount, 2)
    }

    func test_invalidatesContextAfterRender() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()

        var escapingContext: RenderContext<ParentWorkflow>!

        _ = manager.render { context -> TestViewModel in
            XCTAssertTrue(context.isValid)
            escapingContext = context
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }
        manager.enableEvents()

        XCTAssertFalse(escapingContext.isValid)
    }

    // A worker declared on a first `render` pass that is not on a subsequent should have the work cancelled.
    func test_cancelsWorkers() {
        struct WorkerWorkflow: Workflow {
            var startExpectation: XCTestExpectation
            var endExpectation: XCTestExpectation

            enum State {
                case notWorking
                case working
            }

            func makeInitialState() -> WorkerWorkflow.State {
                return .notWorking
            }

            func render(state: WorkerWorkflow.State, context: RenderContext<WorkerWorkflow>) -> Bool {
                switch state {
                case .notWorking:
                    return false
                case .working:
                    context.awaitResult(
                        for: ExpectingWorker(
                            startExpectation: startExpectation,
                            endExpectation: endExpectation
                        ),
                        outputMap: { output -> AnyWorkflowAction<WorkerWorkflow> in
                            AnyWorkflowAction.noAction
                        }
                    )
                    return true
                }
            }

            struct ExpectingWorker: Worker {
                var startExpectation: XCTestExpectation
                var endExpectation: XCTestExpectation

                typealias Output = Void

                func run() -> SignalProducer<Void, Never> {
                    return SignalProducer<Void, Never>({ [weak startExpectation, weak endExpectation] observer, lifetime in
                        lifetime.observeEnded {
                            endExpectation?.fulfill()
                        }

                        startExpectation?.fulfill()
                    })
                }

                func isEquivalent(to otherWorker: WorkerWorkflow.ExpectingWorker) -> Bool {
                    return true
                }
            }
        }

        let startExpectation = XCTestExpectation()
        let endExpectation = XCTestExpectation()
        let manager = WorkflowNode<WorkerWorkflow>.SubtreeManager()

        let isRunning = manager.render { context -> Bool in
            WorkerWorkflow(
                startExpectation: startExpectation,
                endExpectation: endExpectation
            )
            .render(
                state: .working,
                context: context
            )
        }

        XCTAssertEqual(true, isRunning)
        wait(for: [startExpectation], timeout: 1.0)

        let isStillRunning = manager.render { context -> Bool in
            WorkerWorkflow(
                startExpectation: startExpectation,
                endExpectation: endExpectation
            )
            .render(
                state: .notWorking,
                context: context
            )
        }

        XCTAssertFalse(isStillRunning)
        wait(for: [endExpectation], timeout: 1.0)
    }

    func test_subscriptionsUnsubscribe() {
        struct SubscribingWorkflow: Workflow {
            var signal: Signal<Void, Never>?

            struct State {}

            func makeInitialState() -> SubscribingWorkflow.State {
                return State()
            }

            func render(state: SubscribingWorkflow.State, context: RenderContext<SubscribingWorkflow>) -> Bool {
                if let signal = signal {
                    context.awaitResult(for: signal.asWorker(key: "signal")) { _ -> AnyWorkflowAction<SubscribingWorkflow> in
                        AnyWorkflowAction.noAction
                    }
                    return true
                } else {
                    return false
                }
            }
        }

        let emittedExpectation = XCTestExpectation()
        let notEmittedExpectation = XCTestExpectation()
        notEmittedExpectation.isInverted = true

        let manager = WorkflowNode<SubscribingWorkflow>.SubtreeManager()
        manager.onUpdate = { output in
            emittedExpectation.fulfill()
        }

        let (signal, observer) = Signal<Void, Never>.pipe()

        let isSubscribing = manager.render { context -> Bool in
            SubscribingWorkflow(
                signal: signal
            )
            .render(
                state: SubscribingWorkflow.State(),
                context: context
            )
        }
        manager.enableEvents()

        XCTAssertTrue(isSubscribing)
        observer.send(value: ())
        wait(for: [emittedExpectation], timeout: 1.0)

        manager.onUpdate = { output in
            notEmittedExpectation.fulfill()
        }

        let isStillSubscribing = manager.render { context -> Bool in
            SubscribingWorkflow(
                signal: nil
            )
            .render(
                state: SubscribingWorkflow.State(),
                context: context
            )
        }
        manager.enableEvents()

        XCTAssertFalse(isStillSubscribing)

        observer.send(value: ())
        wait(for: [notEmittedExpectation], timeout: 1.0)
    }

    // MARK: - SideEffect

    func test_maintainsSideEffectLifetimeBetweenRenderPasses() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()
        XCTAssertTrue(manager.sideEffectLifetimes.isEmpty)

        _ = manager.render { context -> TestViewModel in
            context.runSideEffect(key: "helloWorld") { _ in }
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.sideEffectLifetimes.count, 1)
        let sideEffectKey = manager.sideEffectLifetimes.values.first!

        _ = manager.render { context -> TestViewModel in
            context.runSideEffect(key: "helloWorld") { _ in }
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.sideEffectLifetimes.count, 1)
        XCTAssertTrue(manager.sideEffectLifetimes.values.first === sideEffectKey)
    }

    func test_endsUnusedSideEffectLifetimeAfterRenderPasses() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager()
        XCTAssertTrue(manager.sideEffectLifetimes.isEmpty)

        let lifetimeEndedExpectation = expectation(description: "Lifetime Ended Expectations")
        _ = manager.render { context -> TestViewModel in
            context.runSideEffect(key: "helloWorld") { lifetime in
                lifetime.onEnded {
                    // Capturing `lifetime` to make sure a retain-cycle will still trigger the `onEnded` block
                    print("\(lifetime)")
                    lifetimeEndedExpectation.fulfill()
                }
            }
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.sideEffectLifetimes.count, 1)

        _ = manager.render { context -> TestViewModel in
            context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.noAction }
            )
        }

        XCTAssertEqual(manager.sideEffectLifetimes.count, 0)
        wait(for: [lifetimeEndedExpectation], timeout: 1)
    }
}

private struct TestViewModel {
    var onTap: () -> Void
    var onToggle: () -> Void
}

private struct ParentWorkflow: Workflow {
    struct State {}
    typealias Event = TestWorkflow.Output
    typealias Output = Never

    func makeInitialState() -> State {
        return State()
    }

    func render(state: State, context: RenderContext<ParentWorkflow>) -> Never {
        fatalError()
    }
}

private struct TestWorkflow: Workflow {
    enum State {
        case foo
        case bar
    }

    enum Event: WorkflowAction {
        typealias WorkflowType = TestWorkflow

        case changeState
        case sendOutput

        func apply(toState state: inout TestWorkflow.State) -> TestWorkflow.Output? {
            switch self {
            case .changeState:
                switch state {
                case .foo: state = .bar
                case .bar: state = .foo
                }
                return nil
            case .sendOutput:
                return .helloWorld
            }
        }
    }

    enum Output {
        case helloWorld
    }

    func makeInitialState() -> State {
        return .foo
    }

    func render(state: State, context: RenderContext<TestWorkflow>) -> TestViewModel {
        let sink = context.makeSink(of: Event.self)

        return TestViewModel(
            onTap: { sink.send(.sendOutput) },
            onToggle: { sink.send(.changeState) }
        )
    }
}
