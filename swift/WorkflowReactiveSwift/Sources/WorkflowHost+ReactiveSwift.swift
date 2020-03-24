import ReactiveSwift
import Workflow

extension WorkflowHost: ReactiveExtensionsProvider {}

extension Reactive where Base: WorkflowHostConvertible {

    public var output: Signal<Base.WorkflowType.Output, Never> {
        let host = base.asWorkflowHost()
        return Signal<Base.WorkflowType.Output, Never> { observer, lifetime in
            let hostObserver = AnyWorkflowHostObserver<Base.WorkflowType>(
                onRender: { _,_ in },
                onOutput: { observer.send(value: $1) })
            let token = host.add(observer: hostObserver)
            lifetime.observeEnded { [weak host] in
                host?.removeObserver(for: token)
            }
        }
    }

    public var rendering: Property<Base.WorkflowType.Rendering> {
        let host = base.asWorkflowHost()
        let signal = Signal<Base.WorkflowType.Rendering, Never> { observer, lifetime in
            let hostObserver = AnyWorkflowHostObserver<Base.WorkflowType>(
                onRender: { observer.send(value: $1) },
                onOutput: { _,_ in })
            let token = host.add(observer: hostObserver)
            lifetime.observeEnded { [weak host] in
                host?.removeObserver(for: token)
            }
        }

        return Property(initial: host.rendering, then: signal)
    }

}
