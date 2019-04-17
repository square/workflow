/// `SideEffectContext` collects side effects from a Workflow starting,
/// changing, or a `WorkflowAction` being applied.
///
/// This type only collects side effects, which are then performed by the
/// workflow runtime via the workflow’s `perform(sideEffect:)`.
public struct SideEffectContext<WorkflowType> where WorkflowType: Workflow {

    /// The side effects requested into this context
    var sideEffects: [WorkflowType.SideEffect]

    /// Creates an empty side effect context
    init() {
        sideEffects = []
    }

    /// Requests the given side effect. During runtime, after an update pass
    /// infrastructure will perform this side effect via the workflow’s
    /// `perform(sideEffect:)`
    public mutating func perform(sideEffect: WorkflowType.SideEffect) {
        sideEffects.append(sideEffect)
    }

}
