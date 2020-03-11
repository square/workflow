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

    private let rootNode: WorkflowNode<WorkflowType>
    
    /// Represents the `Rendering` produced by the root workflow in the hierarchy. New `Rendering` values are produced
    /// as state transitions occur within the hierarchy.
    public private(set) var rendering: WorkflowType.Rendering


    /// Initializes a new host with the given workflow at the root.
    ///
    /// - Parameter workflow: The root workflow in the hierarchy
    /// - Parameter debugger: An optional debugger. If provided, the host will notify the debugger of updates
    ///                       to the workflow hierarchy as state transitions occur.
    public init(workflow: WorkflowType, debugger: WorkflowDebugger? = nil) {
        self.debugger = debugger

        self.rootNode = WorkflowNode(workflow: workflow)

        self.rendering = self.rootNode.render()
        self.rootNode.enableEvents()

        debugger?.didEnterInitialState(snapshot: self.rootNode.makeDebugSnapshot())

        rootNode.onOutput = { [weak self] output in
            self?.handle(output: output)
        }

    }

    /// Update the input for the workflow. Will cause a render pass.
    public func update(workflow: WorkflowType) {
        rootNode.update(workflow: workflow)

        // Treat the update as an "output" from the workflow originating from an external event to force a render pass.
        let output = WorkflowNode<WorkflowType>.Output(
            outputEvent: nil,
            debugInfo: WorkflowUpdateDebugInfo(
                workflowType: "\(WorkflowType.self)",
                kind: .didUpdate(source: .external)))
        handle(output: output)
    }

    private func handle(output: WorkflowNode<WorkflowType>.Output) {
        rendering = rootNode.render()
        renderingListener.listener?(rendering)

        if let outputEvent = output.outputEvent {
            outputListener.listener?(outputEvent)
        }

        debugger?.didUpdate(
            snapshot: rootNode.makeDebugSnapshot(),
            updateInfo: output.debugInfo)

        rootNode.enableEvents()
    }
    
    public let outputListener: ListenerContainer<WorkflowType.Output> = .init()
    public let renderingListener: ListenerContainer<WorkflowType.Rendering> = .init()

}


public class ListenerContainer<Output> {
    public var listener: ((Output) -> Void)?
}
