import ReactiveSwift
import Result

extension WorkflowNode {

    /// Manages the subtree of a workflow. Specifically, this type encapsulates the logic required to update and manage
    /// the lifecycle of nested workflows across multiple render passes.
    internal final class SubtreeManager {

        internal var onUpdate: ((Output) -> Void)? = nil

        /// The scheduler that all updates will run on.
        private let scheduler: Scheduler

        /// Signals from the outside world (e.g. UI)
        private let (childEvent, childEventObserver) = Signal<Signal<Output, NoError>, NoError>.pipe()

        /// The current array of children
        private (set) internal var childWorkflows: [ChildKey:AnyChildWorkflow] = [:]

        private (set) internal var childWorkers: [AnyChildWorker] = []

        private let (lifetime, token) = Lifetime.make()

        init(scheduler: Scheduler) {
            self.scheduler = scheduler

            childEvent
                .flatMap(.latest, { $0 })
                .take(during: lifetime)
                .observe(on: scheduler)
                .observeValues { [weak self] event in
                    self?.handle(output: event)
                }
        }

        /// Performs an update pass using the given closure.
        func render<Rendering>(_ actions: (RenderContext<WorkflowType>) -> Rendering) -> Rendering {

            /// Create a workflow context containing the existing children
            let context = Context(
                scheduler: scheduler,
                originalChildWorkflows: childWorkflows,
                originalChildWorkers: childWorkers)

            let wrapped = RenderContext.make(implementation: context)

            /// Pass the context into the closure to allow a render to take place
            let rendering = actions(wrapped)
            
            wrapped.invalidate()

            /// After the render is complete, assign children using *only the children that were used during the render
            /// pass.* This means that any pre-existing children that were *not* used during the render pass are removed
            /// as a result of this call to `render`.
            self.childWorkflows = context.usedChildWorkflows
            self.childWorkers = context.usedChildWorkers

            /// Finally, merge all of the events together from the new set of children and start listening to them
            for child in self.childWorkflows {
                child.value.onUpdate = { [weak self] output in
                    self?.handle(output: output)
                }
            }

            for worker in self.childWorkers {
                worker.onUpdate = { [weak self] output in
                    self?.handle(output: output)
                }
            }

            let eventSources = Signal
                .merge(context.eventSources)
                .map({ update in
                    return Output.update(update, source: .external)
                })

            childEventObserver.send(value: eventSources)

            /// Return the rendered result
            return rendering
        }

        func makeDebugSnapshot() -> [WorkflowHierarchyDebugSnapshot.Child] {
            return childWorkflows
                .sorted(by: { (lhs, rhs) -> Bool in
                    return lhs.key.key < rhs.key.key
                })
                .map {
                    WorkflowHierarchyDebugSnapshot.Child(
                        key: $0.key.key,
                        snapshot: $0.value.makeDebugSnapshot())
                }
        }

        private func handle(output: Output) {
            onUpdate?(output)
        }


    }

}

extension WorkflowNode.SubtreeManager {

    enum Output {
        case update(AnyWorkflowAction<WorkflowType>, source: WorkflowUpdateDebugInfo.Source)
        case childDidUpdate(WorkflowUpdateDebugInfo)
    }

}

extension WorkflowNode.SubtreeManager {

    /// The workflow context implementation used by the subtree manager.
    fileprivate final class Context: RenderContextType {

        private let scheduler: Scheduler
        
        private let originalChildWorkflows: [ChildKey:AnyChildWorkflow]
        private (set) internal var usedChildWorkflows: [ChildKey:AnyChildWorkflow]

        private let originalChildWorkers: [AnyChildWorker]
        private (set) internal var usedChildWorkers: [AnyChildWorker]

        private (set) internal var eventSources: [Signal<AnyWorkflowAction<WorkflowType>, NoError>] = []

        internal init(scheduler: Scheduler, originalChildWorkflows: [ChildKey:AnyChildWorkflow], originalChildWorkers: [AnyChildWorker]) {
            self.scheduler = scheduler
            self.originalChildWorkflows = originalChildWorkflows
            self.usedChildWorkflows = [:]

            self.originalChildWorkers = originalChildWorkers
            self.usedChildWorkers = []
        }

        func render<Child, Action>(workflow: Child, key: String, outputMap: @escaping (Child.Output) -> Action) -> Child.Rendering where Child : Workflow, Action : WorkflowAction, WorkflowType == Action.WorkflowType {

            /// A unique key used to identify this child workflow
            let childKey = ChildKey(childType: Child.self, key: key)

            /// If the key already exists in `used`, than a workflow of the same type has been rendered multiple times
            /// during this render pass with the same key. This is not allowed.
            guard usedChildWorkflows[childKey] == nil else {
                fatalError("Child workflows of the same type must be given unique keys. Duplicate workflows of type \(Child.self) were encountered with the key \"\(key)\"")
            }

            let child: ChildWorkflow<Child>

            /// See if we can
            if let existing = originalChildWorkflows[childKey] {

                /// Cast the untyped child into a specific typed child. Because our children are keyed by their workflow
                /// type, this should never fail.
                guard let existing = existing as? ChildWorkflow<Child> else {
                    fatalError("ChildKey class type does not match the underlying workflow type.")
                }

                /// Update the existing child
                existing.update(
                    workflow: workflow,
                    outputMap: { AnyWorkflowAction(outputMap($0)) })
                child = existing
            } else {
                /// We could not find an existing child matching the given child key, so we will generate a new child.
                /// This spins up a new workflow node, etc to host the newly created child.
                child = ChildWorkflow<Child>(
                    workflow: workflow,
                    scheduler: scheduler,
                    outputMap: { AnyWorkflowAction(outputMap($0)) })
            }

            /// Store the resolved child in `used`. This allows us to a) hold on to any used children after this render
            /// pass, and b) ensure that we never allow the use of a given workflow type with identical keys.
            usedChildWorkflows[childKey] = child
            return child.render()
        }

        func subscribe<Action>(signal: Signal<Action, NoError>) where Action : WorkflowAction, WorkflowType == Action.WorkflowType {
            eventSources.append(signal.map { AnyWorkflowAction($0) })
        }

        func awaitResult<W, Action>(for worker: W, outputMap: @escaping (W.Output) -> Action) where W : Worker, Action : WorkflowAction, WorkflowType == Action.WorkflowType {

            let outputMap = { AnyWorkflowAction(outputMap($0)) }

            if let existingWorker = originalChildWorkers
                .compactMap({ $0 as? ChildWorker<W> })
                .first(where: { $0.worker.isEquivalent(to: worker) }) {
                existingWorker.update(outputMap: outputMap)
                usedChildWorkers.append(existingWorker)
            } else {
                let newChildWorker = ChildWorker(worker: worker, outputMap: outputMap)
                usedChildWorkers.append(newChildWorker)
            }
        }
        
    }

}

extension WorkflowNode.SubtreeManager {
    
    struct ChildKey: Hashable {

        var childType: Any.Type
        var key: String

        init<T>(childType: T.Type, key: String) {
            self.childType = childType
            self.key = key
        }
        
        func hash(into hasher: inout Hasher) {
            hasher.combine(ObjectIdentifier(childType))
            hasher.combine(key)
        }
        
        static func ==(lhs: ChildKey, rhs: ChildKey) -> Bool {
            return lhs.childType == rhs.childType
                && lhs.key == rhs.key
        }
    }

}

extension WorkflowNode.SubtreeManager {

    /// Abstract base class for running children in the subtree.
    internal class AnyChildWorker {
        var onUpdate: ((Output) -> Void)? = nil
    }

    fileprivate final class ChildWorker<W: Worker>: AnyChildWorker {

        let worker: W

        let signalProducer: SignalProducer<W.Output, NoError>

        private var outputMap: (W.Output) -> AnyWorkflowAction<WorkflowType>

        private var disposable: Disposable? = nil

        init(worker: W, outputMap: @escaping (W.Output) -> AnyWorkflowAction<WorkflowType>) {
            self.worker = worker
            self.signalProducer = worker.run()
            self.outputMap = outputMap
            super.init()

            disposable = signalProducer
                .observe(on: QueueScheduler.workflowExecution)
                .startWithValues { [weak self] output in
                    self?.handle(output: output)
                }
        }

        func update(outputMap: @escaping (W.Output) -> AnyWorkflowAction<WorkflowType>) {
            self.outputMap = outputMap
        }

        private func handle(output: W.Output) {
            let output = Output.update(
                outputMap(output),
                source: .worker)
            onUpdate?(output)
        }

    }




}

extension WorkflowNode.SubtreeManager {

    /// Abstract base class for running children in the subtree.
    internal class AnyChildWorkflow {

        var onUpdate: ((Output) -> Void)? = nil

        func makeDebugSnapshot() -> WorkflowHierarchyDebugSnapshot {
            fatalError()
        }
        
    }
    
    fileprivate final class ChildWorkflow<W: Workflow>: AnyChildWorkflow {
        
        private let node: WorkflowNode<W>
        private var outputMap: (W.Output) -> AnyWorkflowAction<WorkflowType>
        
        private let (lifetime, token) = Lifetime.make()
        
        init(workflow: W, scheduler: Scheduler, outputMap: @escaping (W.Output) -> AnyWorkflowAction<WorkflowType>) {
            self.outputMap = outputMap
            self.node = WorkflowNode<W>(workflow: workflow, scheduler: scheduler)

            super.init()

            node.onOutput = { [weak self] output in
                self?.handle(workflowOutput: output)
            }

        }
        
        func render() -> W.Rendering {
            return node.render()
        }
        
        func update(workflow: W, outputMap: @escaping (W.Output) -> AnyWorkflowAction<WorkflowType>) {
            self.outputMap = outputMap
            self.node.update(workflow: workflow)
        }

        private func handle(workflowOutput: WorkflowNode<W>.Output) {

            let output: Output

            if let outputEvent = workflowOutput.outputEvent {
                output = Output.update(
                        outputMap(outputEvent),
                        source: .subtree(workflowOutput.debugInfo))
            } else {
                output =  Output.childDidUpdate(workflowOutput.debugInfo)
            }

            onUpdate?(output)
        }

        override func makeDebugSnapshot() -> WorkflowHierarchyDebugSnapshot {
            return node.makeDebugSnapshot()
        }
        
    }
    
    
}


