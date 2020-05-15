/*
 * Copyright 2020 Square Inc.
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

import ReactiveSwift

/// Defines a node in the workflow tree.
///
/// ***
/// **Initialization and Updating**
/// ***
///
/// A workflow node comes into existence after its parent produces
/// an instance of that workflow and uses it during a render pass (see the
/// `render` method for more details).
///
/// - If this is the first time the parent has rendered a child of
///   this type, a new workflow node is created. The workflow
///   passed in from the parent will be used to invoke
///   `initialState()` to obtain an initial state.
///
/// - If the parent had previously rendered a child of this type, the
///   existing workflow node will be updated.
///   `workflowDidChange(from:state:)` will be invoked
///   to allow the workflow to respond to the change.
///
/// ***
/// **Render**
/// ***
/// After a workflow node has been created, or any time its state changes,
/// a render pass occurs. The render pass takes the workflow that was passed
/// down from the parent along with the current state and generates a value
/// of type `Rendering`. In a common case, a workflow might render to a screen
/// model for display.
///
/// ```
/// func render(state: State, context: RenderContext<Self>) -> MyScreenModel {
///     return MyScreenModel()
/// }
/// ```
///
public protocol Workflow: AnyWorkflowConvertible {
    /// Defines the state that is managed by this workflow.
    associatedtype State

    /// `Output` defines the type that can be emitted as output events.
    associatedtype Output = Never

    /// `Rendering` is the type that is produced by the `render` method: it
    /// is commonly a view / screen model.
    associatedtype Rendering

    /// This method is invoked once when a workflow node comes into existence.
    ///
    /// - Returns: The initial state for the workflow.
    func makeInitialState() -> State

    /// Called when a new workflow is passed down from the parent to an existing workflow node.
    ///
    /// - Parameter previousWorkflow: The workflow before the update.
    /// - Parameter state: The current state.
    func workflowDidChange(from previousWorkflow: Self, state: inout State)

    /// Called to "render" the current state into `Rendering`. A workflow's `Rendering` type is commonly a view or
    /// screen model.
    ///
    /// - Parameter state: The current state.
    /// - Parameter context: The workflow context is the composition point for the workflow tree. To use a nested
    ///                      workflow, instantiate it based on the current state. The newly instantiated workflow is
    ///                      then used to invoke `context.render(_ workflow:)`, which returns the child's `Rendering`
    ///                      type after creating or updating the nested workflow.
    func render(state: State, context: RenderContext<Self>) -> Rendering
}

extension Workflow {
    public func workflowDidChange(from previousWorkflow: Self, state: inout State) {}
}

/// When State is Void, provide empty `makeInitialState` and `workflowDidChange`
/// implementations, making a “stateless workflow”.
extension Workflow where State == Void {
    public func makeInitialState() -> State {
        return ()
    }

    public func workflowDidChange(from previousWorkflow: Self, state: inout State) {}
}

extension Workflow {
    public func asAnyWorkflow() -> AnyWorkflow<Rendering, Output> {
        return AnyWorkflow(self)
    }
}
