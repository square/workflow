import ReactiveSwift
import Result

/// Manages a running workflow.
final class WorkflowNode<WorkflowType: Workflow> {

    /// Holds the current state of the workflow
    private var state: WorkflowType.State

    /// Holds the current workflow.
    private var workflow: WorkflowType

    var onOutput: ((Output) -> Void)? = nil

    /// Manages the children of this workflow, including diffs during/after render passes.
    private let subtreeManager = SubtreeManager()
    
    init(workflow: WorkflowType) {
        
        /// Get the initial state
        self.workflow = workflow
        self.state = workflow.makeInitialState()

        subtreeManager.onUpdate = { [weak self] output in
            self?.handle(subtreeOutput: output)
        }

    }

    /// Handles an event produced by the subtree manager
    private func handle(subtreeOutput: SubtreeManager.Output) {

        let output: Output

        switch subtreeOutput {
        case .update(let event, let source):
            /// Apply the update to the current state
            let outputEvent = event.apply(toState: &state)

            /// Finally, we tell the outside world that our state has changed (including an output event if it exists).
            output = Output(
                outputEvent: outputEvent,
                debugInfo: WorkflowUpdateDebugInfo(
                    workflowType: "\(WorkflowType.self)",
                    kind: .didUpdate(source: source)))

        case .childDidUpdate(let debugInfo):
            output = Output(
                    outputEvent: nil,
                    debugInfo: WorkflowUpdateDebugInfo(
                        workflowType: "\(WorkflowType.self)",
                        kind: .childDidUpdate(debugInfo)))
        }

        onOutput?(output)
    }

    func render() -> WorkflowType.Rendering {
        return subtreeManager.render { context in
            return workflow
                .render(
                    state: state,
                    context: context)
        }
    }

    func enableEvents() {
        subtreeManager.enableEvents()
    }

    /// Updates the workflow.
    func update(workflow: WorkflowType) {
        workflow.workflowDidChange(from: self.workflow, state: &state)
        self.workflow = workflow
    }

    func makeDebugSnapshot() -> WorkflowHierarchyDebugSnapshot {
        return WorkflowHierarchyDebugSnapshot(
            workflowType: "\(WorkflowType.self)",
            stateDescription: "\(state)",
            children: subtreeManager.makeDebugSnapshot())
    }
    
}

extension WorkflowNode {

    struct Output {
        var outputEvent: WorkflowType.Output?
        var debugInfo: WorkflowUpdateDebugInfo
    }

}
