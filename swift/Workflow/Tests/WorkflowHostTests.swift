import XCTest
@testable import Workflow

import ReactiveSwift
import Result


final class WorkflowHostTests: XCTestCase {

    func test_hostRendersSynchronouslyOnAction() {
        let host = WorkflowHost(workflow: TestWorkflow())

        var screen = host.rendering.value
        XCTAssertEqual(0, screen.count)

        screen.update()

        screen = host.rendering.value
        XCTAssertEqual(1, screen.count)
    }

    // When the workflow host is synchronous, it must prevent reentrant calls (actions being sent during a render pass).
    // This is to prevent excessive stack depths that would block UI updates.
    // The current implementation does not subscribe to signals during the call to `render`, so the events are ignored.
    func test_reentrantCallsIgnored() {
        let host = WorkflowHost(workflow: ReentrantWorkflow())

        var screen = host.rendering.value
        XCTAssertEqual(0, screen.count)

        screen.update()

        screen = host.rendering.value
        XCTAssertEqual(1, screen.count)
    }

    func test_hostRendersAsyncOnAsyncScheduler() {
        let host = WorkflowHost(
            workflow: TestWorkflow(),
            scheduler: QueueScheduler.workflowExecution)

        let expecation = XCTestExpectation()
        host.rendering.signal.observeValues { screen in
            if screen.count == 1 {
                expecation.fulfill()
            }
        }

        var screen = host.rendering.value
        XCTAssertEqual(0, screen.count)

        screen.update()

        screen = host.rendering.value
        XCTAssertEqual(0, screen.count)

        wait(for: [expecation], timeout: 1.0)
    }
}


fileprivate struct TestWorkflow: Workflow {
    struct State {
        var count: Int
    }

    func makeInitialState() -> TestWorkflow.State {
        return State(count: 0)
    }

    func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout TestWorkflow.State) {

    }

    struct Screen {
        var count: Int
        var update: () -> Void
    }
    typealias Rendering = Screen

    func render(state: TestWorkflow.State, context: RenderContext<TestWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        return Screen(count: state.count, update: { sink.send(.update) })
    }

    enum Action: WorkflowAction {
        typealias WorkflowType = TestWorkflow

        case update

        func apply(toState state: inout TestWorkflow.State) -> TestWorkflow.Output? {
            switch self {
            case .update:
                state.count += 1
                return nil
            }
        }
    }
}


fileprivate struct ReentrantWorkflow: Workflow {
    struct State {
        var count: Int
    }

    func makeInitialState() -> ReentrantWorkflow.State {
        return State(count: 0)
    }

    func workflowDidChange(from previousWorkflow: ReentrantWorkflow, state: inout ReentrantWorkflow.State) {

    }

    struct Screen {
        var count: Int
        var update: () -> Void
    }
    typealias Rendering = Screen

    func render(state: ReentrantWorkflow.State, context: RenderContext<ReentrantWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        if state.count == 0 {
            sink.send(.update)
        }

        return Screen(
            count: state.count,
            update: { sink.send(.update) })
    }

    enum Action: WorkflowAction {
        typealias WorkflowType = ReentrantWorkflow

        case update

        func apply(toState state: inout ReentrantWorkflow.State) -> ReentrantWorkflow.Output? {
            switch self {
            case .update:
                state.count += 1
                return nil
            }
        }
    }
}
