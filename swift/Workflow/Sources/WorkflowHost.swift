import ReactiveSwift
import Result

/// Defines a type that receives debug information about a running workflow hierarchy.
public protocol WorkflowDebugger {

    /// Called once when the workflow hierarchy initializes.
    ///
    /// - Parameter snapshot: Debug information about the workflow hierarchy.
    func didEnterInitialState(snapshot: WorkflowHierarchyDebugSnapshot)

    /// Called when an update occurs anywhere within the workflow hierarchy.
    ///
    /// - Parameter snapshot: Debug information about the workflow hierarchy *after* the update.
    /// - Parameter updateInfo: Information about the update.
    func didUpdate(snapshot: WorkflowHierarchyDebugSnapshot, updateInfo: WorkflowUpdateDebugInfo)
}

/// Manages an active workflow hierarchy.
public final class WorkflowHost<WorkflowType: Workflow> {

    private let debugger: WorkflowDebugger?

    private let (outputEvent, outputEventObserver) = Signal<WorkflowType.Output, NoError>.pipe()

    private let rootNode: WorkflowNode<WorkflowType>

    private let mutableRendering: MutableProperty<WorkflowType.Rendering>

    /// Represents the `Rendering` produced by the root workflow in the hierarchy. New `Rendering` values are produced
    /// as state transitions occur within the hierarchy.
    public let rendering: Property<WorkflowType.Rendering>

    /// Initializes a new host with the given workflow at the root.
    ///
    /// - Parameter workflow: The root workflow in the hierarchy
    /// - Parameter debugger: An optional debugger. If provided, the host will notify the debugger of updates
    ///                       to the workflow hierarchy as state transitions occur.
    public init(workflow: WorkflowType, debugger: WorkflowDebugger? = nil) {
        self.debugger = debugger

        self.rootNode = WorkflowNode(workflow: workflow)

        self.mutableRendering = MutableProperty(self.rootNode.render())
        self.rendering = Property(mutableRendering)

        debugger?.didEnterInitialState(snapshot: self.rootNode.makeDebugSnapshot())

        rootNode.onOutput = { [weak self] output in
            self?.handle(output: output)
        }

    }

    private func handle(output: WorkflowNode<WorkflowType>.Output) {
        mutableRendering.value = rootNode.render()

        if let outputEvent = output.outputEvent {
            outputEventObserver.send(value: outputEvent)
        }

        debugger?.didUpdate(
            snapshot: rootNode.makeDebugSnapshot(),
            updateInfo: output.debugInfo)
    }

    /// A signal containing output events emitted by the root workflow in the hierarchy.
    public var output: Signal<WorkflowType.Output, NoError> {
        return outputEvent
    }

}
