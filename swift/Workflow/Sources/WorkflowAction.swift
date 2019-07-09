/// Conforming types represent an action that advances a workflow. When applied, an action emits the next
/// state and / or output for the workflow.
public protocol WorkflowAction {

    /// The type of workflow that this action can be applied to.
    associatedtype WorkflowType: Workflow

    /// Applies this action to a given state of the workflow, optionally returning an output event.
    ///
    /// - Parameter state: The current state of the workflow. The state is passed as an `inout` param, allowing actions
    ///                    to modify state during application.
    ///
    /// - Returns: An optional output event for the workflow. If an output event is returned, it will be passed up
    ///            the workflow hierarchy to this workflow's parent.
    func apply(toState state: inout WorkflowType.State) -> WorkflowType.Output?

}


/// A type-erased workflow action.
///
/// The `AnyWorkflowAction` type forwards `apply` to an underlying workflow action, hiding its specific underlying type,
/// or to a closure that implements the `apply` logic.
public struct AnyWorkflowAction<WorkflowType: Workflow>: WorkflowAction {

    private let _apply: (inout WorkflowType.State) -> WorkflowType.Output?

    /// Creates a type-erased workflow action that wraps the given instance.
    ///
    /// - Parameter base: A workflow action to wrap.
    public init<E>(_ base: E) where E: WorkflowAction, E.WorkflowType == WorkflowType {
        if let anyEvent = base as? AnyWorkflowAction<WorkflowType> {
            self = anyEvent
            return
        }
        _apply = { return base.apply(toState: &$0) }
    }

    /// Creates a type-erased workflow action with the given `apply` implementation.
    ///
    /// - Parameter apply: the apply function for the resulting action.
    public init(_ apply: @escaping (inout WorkflowType.State) -> WorkflowType.Output?) {
        _apply = apply
    }

    public func apply(toState state: inout WorkflowType.State) -> WorkflowType.Output? {
        return _apply(&state)
    }

}

extension AnyWorkflowAction {

    /// Creates a type-erased workflow action that simply sends the given output event.
    ///
    /// - Parameter output: The output event to send when this action is applied.
    public init(sendingOutput output: WorkflowType.Output) {
        self = AnyWorkflowAction { state in
            return output
        }
    }

    /// Creates a type-erased workflow action that does nothing (it leaves state unchanged and does not emit an output
    /// event).
    public static var noAction: AnyWorkflowAction<WorkflowType> {
        return AnyWorkflowAction { state in
            return nil
        }
    }

}
