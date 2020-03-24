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
import Foundation

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

public protocol WorkflowHostConvertible {

    associatedtype WorkflowType: Workflow

    func asWorkflowHost() -> WorkflowHost<WorkflowType>

}


public protocol WorkflowHostObserver: class {

    associatedtype WorkflowType: Workflow

    func workflowHost(host: WorkflowHost<WorkflowType>, didRender rendering: WorkflowType.Rendering)

    func workflowHost(host: WorkflowHost<WorkflowType>, didOutput output: WorkflowType.Output)

}


/// Manages an active workflow hierarchy.
public final class WorkflowHost<WorkflowType: Workflow>: WorkflowHostConvertible {

    public func asWorkflowHost() -> WorkflowHost<WorkflowType> {
        return self
    }

    private let debugger: WorkflowDebugger?

    private let rootNode: WorkflowNode<WorkflowType>

    public private(set) var rendering: WorkflowType.Rendering

    private var observers: [ObserverToken: AnyWorkflowHostObserver<WorkflowType>] = [:]

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

    public func add<ObserverType>(observer: ObserverType) -> ObserverToken where ObserverType: WorkflowHostObserver, ObserverType.WorkflowType == WorkflowType {
        let token = ObserverToken()
        if let observer = observer as? AnyWorkflowHostObserver<WorkflowType> {
            observers[token] = observer
        } else {
            observers[token] = AnyWorkflowHostObserver(observer)

        }
        return token
    }

    public func removeObserver(for token: ObserverToken) {
        observers[token] = nil
    }

    private func handle(output: WorkflowNode<WorkflowType>.Output) {
        rendering = rootNode.render()
        notifyRendering(rendering: rendering)

        if let outputEvent = output.outputEvent {
            notifyOutput(output: outputEvent)
        }

        debugger?.didUpdate(
            snapshot: rootNode.makeDebugSnapshot(),
            updateInfo: output.debugInfo)

        rootNode.enableEvents()
    }

    private func notifyRendering(rendering: WorkflowType.Rendering) {
        for (_, observer) in observers {
            observer.workflowHost(host: self, didRender: rendering)
        }
    }

    private func notifyOutput(output: WorkflowType.Output) {
        for (_, observer) in observers {
            observer.workflowHost(host: self, didOutput: output)
        }
    }

}


extension WorkflowHost {

    public struct ObserverToken: Hashable {

        private var uuid: UUID

        init() {
            uuid = UUID()
        }
    }

}


public final class AnyWorkflowHostObserver<WorkflowType: Workflow>: WorkflowHostObserver {

    private let onRender: (WorkflowHost<WorkflowType>, WorkflowType.Rendering) -> Void
    private let onOutput: (WorkflowHost<WorkflowType>,WorkflowType.Output) -> Void

    public convenience init<ObserverType>(_ observer: ObserverType) where ObserverType: WorkflowHostObserver, ObserverType.WorkflowType == WorkflowType {
        self.init(
            onRender: { [weak observer] host, rendering in
                observer?.workflowHost(host: host, didRender: rendering)
            },
            onOutput: { [weak observer] host, output in
                observer?.workflowHost(host: host, didOutput: output)
            }
        )
    }

    public init(onRender: @escaping (WorkflowHost<WorkflowType>, WorkflowType.Rendering) -> Void, onOutput: @escaping (WorkflowHost<WorkflowType>,WorkflowType.Output) -> Void) {
        self.onRender = onRender
        self.onOutput = onOutput
    }

    public func workflowHost(host: WorkflowHost<WorkflowType>, didRender rendering: WorkflowType.Rendering) {
        onRender(host, rendering)
    }

    public func workflowHost(host: WorkflowHost<WorkflowType>, didOutput output: WorkflowType.Output) {
        onOutput(host, output)
    }

}
