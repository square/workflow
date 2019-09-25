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


final class WorkflowNodeTests: XCTestCase {

    func test_rendersSimpleWorkflow() {
        let node = WorkflowNode(workflow: SimpleWorkflow(string: "Foo"))
        XCTAssertEqual(node.render(), "ooF")
    }

    func test_rendersNestedWorkflows() {

        let node = WorkflowNode(
            workflow: CompositeWorkflow(
                a: SimpleWorkflow(string: "Hello"),
                b: SimpleWorkflow(string: "World")))

        XCTAssertEqual(node.render().aRendering, "olleH")
        XCTAssertEqual(node.render().bRendering, "dlroW")
    }

    func test_childWorkflowsEmitOutputEvents() {

        typealias WorkflowType = CompositeWorkflow<EventEmittingWorkflow, SimpleWorkflow>

        let workflow = CompositeWorkflow(
            a: EventEmittingWorkflow(string: "Hello"),
            b: SimpleWorkflow(string: "World"))

        let node = WorkflowNode(workflow: workflow)

        let rendering = node.render()
        node.enableEvents()

        var outputs: [WorkflowType.Output] = []

        let expectation = XCTestExpectation(description: "Node output")

        node.onOutput = { value in
            if let output = value.outputEvent {
                outputs.append(output)
                expectation.fulfill()
            }
        }

        rendering.aRendering.someoneTappedTheButton()

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(outputs, [WorkflowType.Output.childADidSomething(.helloWorld)])
    }

    func test_childWorkflowsEmitStateChangeEvents() {

        typealias WorkflowType = CompositeWorkflow<StateTransitioningWorkflow, SimpleWorkflow>

        let workflow = CompositeWorkflow(
            a: StateTransitioningWorkflow(),
            b: SimpleWorkflow(string: "World"))

        let node = WorkflowNode(workflow: workflow)

        let expectation = XCTestExpectation(description: "State Change")
        var stateChangeCount = 0

        node.onOutput = { _ in
            stateChangeCount += 1
            if stateChangeCount == 3 {
                expectation.fulfill()
            }
        }

        var aRendering = node.render().aRendering
        node.enableEvents()
        aRendering.toggle()

        aRendering = node.render().aRendering
        node.enableEvents()
        aRendering.toggle()

        aRendering = node.render().aRendering
        node.enableEvents()
        aRendering.toggle()

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(stateChangeCount, 3)
    }

    func test_debugUpdateInfo() {

        typealias WorkflowType = CompositeWorkflow<EventEmittingWorkflow, SimpleWorkflow>

        let workflow = CompositeWorkflow(
            a: EventEmittingWorkflow(string: "Hello"),
            b: SimpleWorkflow(string: "World"))

        let node = WorkflowNode(workflow: workflow)

        let rendering = node.render()
        node.enableEvents()

        var emittedDebugInfo: [WorkflowUpdateDebugInfo] = []

        let expectation = XCTestExpectation(description: "Output")
        node.onOutput = { value in
            emittedDebugInfo.append(value.debugInfo)
            expectation.fulfill()
        }

        rendering.aRendering.someoneTappedTheButton()

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(emittedDebugInfo.count, 1)

        let debugInfo = emittedDebugInfo[0]

        XCTAssert(debugInfo.workflowType == "\(WorkflowType.self)")

        /// Test the shape of the emitted debug info
        switch debugInfo.kind {
        case .childDidUpdate(_):
            XCTFail()
        case .didUpdate(let source):
            switch source {
            case .external, .worker:
                XCTFail()
            case .subtree(let childInfo):
                XCTAssert(childInfo.workflowType == "\(EventEmittingWorkflow.self)")
                switch childInfo.kind {
                case .childDidUpdate(_):
                    XCTFail()
                case .didUpdate(let source):
                    switch source {
                    case .external:
                        break
                    case .subtree(_), .worker:
                        XCTFail()
                    }
                }

            }

        }

    }

    func test_debugTreeSnapshots() {

        typealias WorkflowType = CompositeWorkflow<EventEmittingWorkflow, SimpleWorkflow>

        let workflow = CompositeWorkflow(
            a: EventEmittingWorkflow(string: "Hello"),
            b: SimpleWorkflow(string: "World"))
        let node = WorkflowNode(workflow: workflow)
        _ = node.render() // the debug snapshow always reflects the tree after the latest render pass

        let snapshot = node.makeDebugSnapshot()

        let expectedSnapshot = WorkflowHierarchyDebugSnapshot(
            workflowType: "\(WorkflowType.self)",
            stateDescription: "\(WorkflowType.State())",
            children: [
                WorkflowHierarchyDebugSnapshot.Child(
                    key: "a",
                    snapshot: WorkflowHierarchyDebugSnapshot(
                        workflowType: "\(EventEmittingWorkflow.self)",
                        stateDescription: "\(EventEmittingWorkflow.State())")),
                WorkflowHierarchyDebugSnapshot.Child(
                    key: "b",
                    snapshot: WorkflowHierarchyDebugSnapshot(
                        workflowType: "\(SimpleWorkflow.self)",
                        stateDescription: "\(SimpleWorkflow.State())"))
            ])

        XCTAssertEqual(snapshot, expectedSnapshot)

    }

    func test_handlesRepeatedWorkerOutputs() {

        struct WF: Workflow {

            struct State {}

            typealias Output = Int
            
            typealias Rendering = Void

            func makeInitialState() -> WF.State {
                return State()
            }

            func workflowDidChange(from previousWorkflow: WF, state: inout WF.State) {

            }

            func render(state: WF.State, context: RenderContext<WF>) -> Void {
                context.awaitResult(for: TestWorker()) { output in
                    return AnyWorkflowAction(sendingOutput: output)
                }
            }
        }

        struct TestWorker: Worker {
            func isEquivalent(to otherWorker: TestWorker) -> Bool {
                return true
            }

            func run() -> SignalProducer<Int, Never> {
                return SignalProducer{ observer, lifetime in
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: {
                        observer.send(value: 1)
                        observer.send(value: 2)
                        observer.sendCompleted()
                    })
                }
            }
        }

        let expectation = XCTestExpectation(description: "Test Worker")
        var outputs: [Int] = []

        let node = WorkflowNode(workflow: WF())
        node.onOutput = { output in
            if let outputInt = output.outputEvent {
                outputs.append(outputInt)

                if outputs.count == 2 {
                    expectation.fulfill()
                }
            }
        }

        node.render()
        node.enableEvents()

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(outputs, [1, 2])
    }

}

/// Renders two child state machines of types `A` and `B`.
fileprivate struct CompositeWorkflow<A, B>: Workflow where
    A: Workflow,
    B: Workflow {

    var a: A
    var b: B
}

extension CompositeWorkflow {

    struct State {}

    struct Rendering {
        var aRendering: A.Rendering
        var bRendering: B.Rendering
    }

    enum Output {
        case childADidSomething(A.Output)
        case childBDidSomething(B.Output)
    }

    enum Event: WorkflowAction {
        case a(A.Output)
        case b(B.Output)

        typealias WorkflowType = CompositeWorkflow<A, B>

        func apply(toState state: inout CompositeWorkflow<A, B>.State) -> CompositeWorkflow<A, B>.Output? {
            switch self {
            case .a(let childOutput):
                return .childADidSomething(childOutput)
            case .b(let childOutput):
                return .childBDidSomething(childOutput)
            }
        }
    }

    func makeInitialState() -> CompositeWorkflow<A, B>.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: CompositeWorkflow<A, B>, state: inout State) {

    }



    func render(state: State, context: RenderContext<CompositeWorkflow<A, B>>) -> Rendering {


        return Rendering(
            aRendering: a
                .mapOutput { Event.a($0) }
                .rendered(with: context, key: "a"),
            bRendering: b
                .mapOutput { Event.b($0) }
                .rendered(with: context, key: "b"))
    }

}

extension CompositeWorkflow.Rendering: Equatable where A.Rendering: Equatable, B.Rendering: Equatable {
    fileprivate static func ==(lhs: CompositeWorkflow.Rendering, rhs: CompositeWorkflow.Rendering) -> Bool {
        return lhs.aRendering == rhs.aRendering
            && lhs.bRendering == rhs.bRendering
    }
}

extension CompositeWorkflow.Output: Equatable where A.Output: Equatable, B.Output: Equatable {
    fileprivate static func ==(lhs: CompositeWorkflow.Output, rhs: CompositeWorkflow.Output) -> Bool {
        switch (lhs, rhs) {
        case (.childADidSomething(let l), .childADidSomething(let r)):
            return l == r
        case (.childBDidSomething(let l), .childBDidSomething(let r)):
            return l == r
        default:
            return false
        }
    }
}

/// Has no state or output, simply renders a reversed string
fileprivate struct SimpleWorkflow: Workflow {
    var string: String

    struct State {}

    func makeInitialState() -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: SimpleWorkflow, state: inout State) {

    }

    func render(state: State, context: RenderContext<SimpleWorkflow>) -> String {
        return String(string.reversed())
    }

}


/// Renders to a model that contains a callback, which in turn sends an output event.
fileprivate struct EventEmittingWorkflow: Workflow {
    var string: String
}

extension EventEmittingWorkflow {

    struct State {

    }

    struct Rendering {
        var someoneTappedTheButton: () -> Void
    }

    func makeInitialState() -> State {
        return State()
    }

    enum Event: Equatable, WorkflowAction {
        case tapped

        typealias WorkflowType = EventEmittingWorkflow

        func apply(toState state: inout EventEmittingWorkflow.State) -> EventEmittingWorkflow.Output? {
            switch self {
            case .tapped:
                return .helloWorld
            }
        }
    }

    enum Output: Equatable {
        case helloWorld
    }

    func workflowDidChange(from previousWorkflow: EventEmittingWorkflow, state: inout State) {

    }

    func render(state: State, context: RenderContext<EventEmittingWorkflow>) -> Rendering {


        let sink = context.makeSink(of: Event.self)

        return Rendering(someoneTappedTheButton: { sink.send(.tapped) })
    }
}


/// Renders to a model that contains a callback, which in turn sends an output event.
fileprivate struct StateTransitioningWorkflow: Workflow {

    typealias State = Bool
    
    typealias Output = Never

    struct Rendering {
        var toggle: () -> Void
        var currentValue: Bool
    }

    func makeInitialState() -> Bool {
        return false
    }

    func workflowDidChange(from previousWorkflow: StateTransitioningWorkflow, state: inout Bool) {
        
    }

    func render(state: State, context: RenderContext<StateTransitioningWorkflow>) -> Rendering {

        let sink = context.makeSink(of: Event.self)

        return Rendering(
            toggle: { sink.send(.toggle) },
            currentValue: state)
    }

    enum Event: WorkflowAction {
        case toggle

        typealias WorkflowType = StateTransitioningWorkflow

        func apply(toState state: inout Bool) -> Never? {
            switch self {
            case .toggle:
                state.toggle()
            }
            return nil
        }
    }


}

#if compiler(>=5.0)
// Never gains Equatable and Hashable conformance in Swift 5
#else
extension Never: Equatable {}
#endif
