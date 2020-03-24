import Combine
import Workflow


extension Publisher where Failure == Never {

    public func running<Parent, Action>(with context: RenderContext<Parent>, key: String = "", outputMap: @escaping (Output) -> Action) where Action: WorkflowAction, Action.WorkflowType == Parent {
        _ = eraseToAnyPublisher()
            .asAnyWorkflow()
            .rendered(with: context, key: key, outputMap: outputMap)
    }

}

extension Publisher where Output: WorkflowAction, Failure == Never {

    public func running<Parent>(with context: RenderContext<Parent>, key: String = "") where Parent: Workflow, Output.WorkflowType == Parent {
        eraseToAnyPublisher()
            .asAnyWorkflow()
            .running(with: context, key: key)
    }

}

extension AnyPublisher: AnyWorkflowConvertible where Failure == Never {

    public func asAnyWorkflow() -> AnyWorkflow<Void, Output> {
        return AnyPublisherWorkflow(publisher: self).asAnyWorkflow()
    }

}


private struct AnyPublisherWorkflow<Value>: Workflow {
    var publisher: AnyPublisher<Value, Never>
    typealias Output = Value
    typealias State = Void
    func makeInitialState() -> State {
        return ()
    }
    func workflowDidChange(from previousWorkflow: AnyPublisherWorkflow, state: inout State) {
    }
    typealias Rendering = Void
    func render(state: State, context: RenderContext<AnyPublisherWorkflow>) -> Rendering {
        let sink = context.makeSink(of: AnyWorkflowAction.self)
        context.runSideEffect(key: "") { [publisher] lifetime in
            let cancellable = publisher
                .map { AnyWorkflowAction(sendingOutput: $0) }
                .subscribe(on: DispatchQueue.workflowExecution)
                .sink(receiveValue: sink.send)
            lifetime.onEnded {
                cancellable.cancel()
            }
        }
        return ()
    }
}
