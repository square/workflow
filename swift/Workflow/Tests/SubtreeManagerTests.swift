import XCTest
import ReactiveSwift
@testable import Workflow


final class SubtreeManagerTests: XCTestCase {
    let scheduler = UIScheduler()

    func test_maintainsChildrenBetweenRenderPasses() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager(scheduler: scheduler)
        XCTAssertTrue(manager.childWorkflows.isEmpty)

        _ = manager.render { context -> TestViewModel in
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.identity })
        }

        XCTAssertEqual(manager.childWorkflows.count, 1)
        let child = manager.childWorkflows.values.first!

        _ = manager.render { context -> TestViewModel in
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.identity })
        }

        XCTAssertEqual(manager.childWorkflows.count, 1)
        XCTAssertTrue(manager.childWorkflows.values.first === child)

    }

    func test_removesUnusedChildrenAfterRenderPasses() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager(scheduler: scheduler)
        _ = manager.render { context -> TestViewModel in
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.identity })
        }

        XCTAssertEqual(manager.childWorkflows.count, 1)

        _ = manager.render { context -> Void in

        }

        XCTAssertTrue(manager.childWorkflows.isEmpty)
    }

    func test_emitsChildEvents() {

        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager(scheduler: scheduler)

        var events: [AnyWorkflowAction<ParentWorkflow>] = []

        manager.onUpdate = {
            switch $0 {
            case .update(let event, _):
                events.append(event)
            default:
                break
            }
        }

        let viewModel = manager.render { context -> TestViewModel in
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.identity })
        }

        viewModel.onTap()
        viewModel.onTap()

        RunLoop.current.run(until: Date().addingTimeInterval(0.1))

        XCTAssertEqual(events.count, 2)

    }

    func test_emitsChangeEvents() {

        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager(scheduler: scheduler)

        var changeCount = 0

        manager.onUpdate = { _ in
            changeCount += 1
        }

        let viewModel = manager.render { context -> TestViewModel in
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.identity })
        }

        viewModel.onToggle()
        viewModel.onToggle()

        RunLoop.current.run(until: Date().addingTimeInterval(0.1))

        XCTAssertEqual(changeCount, 2)
    }
    
    func test_invalidatesContextAfterRender() {
        let manager = WorkflowNode<ParentWorkflow>.SubtreeManager(scheduler: scheduler)
        
        var escapingContext: RenderContext<ParentWorkflow>! = nil
        
        _ = manager.render { context -> TestViewModel in
            XCTAssertTrue(context.isValid)
            escapingContext = context
            return context.render(
                workflow: TestWorkflow(),
                key: "",
                outputMap: { _ in AnyWorkflowAction.identity })
        }
        
        XCTAssertFalse(escapingContext.isValid)
    }

}


fileprivate struct TestViewModel {
    var onTap: () -> Void
    var onToggle: () -> Void
}

fileprivate struct ParentWorkflow: Workflow {
    struct State {}
    typealias Event = TestWorkflow.Output
    typealias Output = Never

    func makeInitialState() -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: ParentWorkflow, state: inout State) {

    }

    func render(state: State, context: RenderContext<ParentWorkflow>) -> Never {
        fatalError()
    }
}


fileprivate struct TestWorkflow: Workflow {

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

    func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout State) {

    }

    func render(state: State, context: RenderContext<TestWorkflow>) -> TestViewModel {

        let sink = context.makeSink(of: Event.self)

        return TestViewModel(
            onTap: { sink.send(.sendOutput) },
            onToggle: { sink.send(.changeState) })
    }

}
