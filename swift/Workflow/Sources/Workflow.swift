import ReactiveSwift
import Result

/// Defines a node in the workflow tree.
///
/// ***
/// **Initialization and Updating**
/// ***
///
/// A workflow node comes into existence after its parent produces
/// an instance of that workflow and uses it during a compose pass (see the
/// `compose` method for more details).
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
/// **Compose**
/// ***
/// After a workflow node has been created, or any time its state changes,
/// a compose pass occurs. The compose pass takes the workflow that was passed
/// down from the parent along with the current state and generates a value
/// of type `Rendering`. In a common case, a workflpw might render to a screen
/// model for display.
///
/// ```
/// func compose(state: State, context: WorkflowContext<Self>) -> MyScreenModel {
///     return MyScreenModel()
/// }
/// ```
///
public protocol Workflow: AnyWorkflowConvertible {

    /// Defines the state that is managed by this workflow.
    associatedtype State

    /// `Output` defines the type that can be emitted as output events.
    associatedtype Output = Never

    /// `Rendering` is the type that is produced by the `compose` method: it
    /// is commonly a view / screen model.
    associatedtype Rendering

    /// `SideEffects` defines work performed in `makeInitialState`,
    /// `workflowDidChange` or from a `WorkflowAction`.
    ///
    /// - Note:
    /// Side effects provide no mechanism for information coming back in to a
    /// Workflow. For work that should affect this workflow’s state, see
    /// `Worker`s.
    associatedtype SideEffect = Never

    /// This method is invoked once when a workflow node comes into existence.
    ///
    /// - Parameter context: The side effect context collects any requested side
    ///                      effects to be performed when this workflow node
    ///                      comes into existence.
    /// - Returns: The initial state for the workflow.
    func makeInitialState(context: inout SideEffectContext<Self>) -> State

    /// Called when a new workflow is passed down from the parent to an existing workflow node.
    ///
    /// - Parameter previousWorkflow: The workflow before the update.
    /// - Parameter state: The current state.
    /// - Parameter context: The side effect context collects any requested side
    ///                      effects to be performed as a result of this
    ///                      workflow change.
    func workflowDidChange(from previousWorkflow: Self, state: inout State, context: inout SideEffectContext<Self>)

    /// Called when this workflow requests a side effect. This is the moment to
    /// do the work described by the given `SideEffect`.
    ///
    /// - Parameter sideEffect: The side effect to be performed.
    func perform(sideEffect: SideEffect)

    /// Called to "compose" the current state into `Rendering`. A workflow's `Rendering` type is commonly a view or
    /// screen model.
    ///
    /// - Parameter state: The current state.
    /// - Parameter context: The workflow context is the composition point for the workflow tree. To use a nested
    ///                      workflow, instantiate it based on the current state. The newly instantiated workflow is
    ///                      then used to invoke `context.render(_ workflow:)`, which returns the child's `Rendering`
    ///                      type after creating or updating the nested workflow.
    func compose(state: State, context: WorkflowContext<Self>) -> Rendering

}

extension Workflow where SideEffect == Never {

    /// When `SideEffect` is `Never`, there is no valid implementation of this
    /// method (it can never be called).
    public func perform(sideEffect: Never) {
    }

}

extension Workflow {

    public func asAnyWorkflow() -> AnyWorkflow<Rendering, Output> {
        return AnyWorkflow(self)
    }

}
