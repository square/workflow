# Concepts

Workflows provide a way to build complex applications out of small, isolated pieces with a predictable data flow and a consistent API contract. They are conceptually similar to components in architecture patterns such as React (though they are fully native and type-safe).

----

*__Note:__ One important difference between workflows and components found in web frontend frameworks comes from the vast differences between the DOM and native UI paradigms (iOS/Android). The DOM is already declarative (meaning that we can always reason about the element tree in a web page). UIKit, for is not – it very much relies on a procedural programming model where transitions are performed by imperative methods like `push`, `fadeOut`, etc. For this reason, workflows do not ever refer directly to views. They are instead responsible for rendering view models. This view model can then be used to update the UI.*

----

### Workflow is cross-platform

While specific APIs differ between Swift and Kotlin, the Workflow library shares all of the same conceptual pieces on both platforms. This is extremely beneficial when building cross-platform software, as the same design (though not the same code) can be used for both Swift and Kotlin. Build a feature on one platform first? That code now serves as an excellent reference when implementing the same functionality on the other platform.


## The Role of a Workflow

`Workflow` is a protocol (in Swift) and interface (in Kotlin) that defines the contract for a single node in the workflow hierarchy.

```swift
public protocol Workflow: AnyWorkflowConvertible {

    associatedtype State

    associatedtype Output = Never

    associatedtype Rendering

    func makeInitialState() -> State

    func workflowDidChange(from previousWorkflow: Self, state: inout State)

    func compose(state: State, context: WorkflowContext<Self>) -> Rendering

}

```

```kotlin
interface Workflow<in I : Any, S : Any, out O : Any, out R : Any> {

  fun initialState(input: I): S

  fun onInputChanged(
    old: I,
    new: I,
    state: S
  ): S = state

  fun compose(
    input: I,
    state: S,
    context: WorkflowContext<S, O>
  ): R

  fun snapshotState(state: S): Snapshot
  fun restoreState(snapshot: Snapshot): S

}

```

Workflows have several responsibilities:

### Workflows have state

Every Workflow implementation defines a `State` type to maintain any necessary state while the workflow is running.

For example, a tic-tac-toe game might have a state like this:

```swift
struct State {

    enum Player {
        case x
        case o
    }

    enum Space {
        case unfilled
        filled(Player)
    }

    // 3 rows * 3 columns = 9 spaces
    var spaces: [Space] = Array(repeating: .unfilled, count: 9)
    var currentTurn: Player = .x
}
```

When the workflow is first started, it is queried for an initial state value. From that point forward, the workflow may advance to a new state as the result of events occurring from various sources (which will be covered below).

### Workflows produce an external representation of their state via `Rendering`

Immediately after starting up, or after a state transition occurs, a workflow will have its `compose(state:context:)` method called. This method is responsible for creating and returning a value of type `Rendering`. You can think of `Rendering` as the "external state" of the workflow. While a workflow's internal state may contain more detailed or comprehensive state, the `Rendering` (external state) is a type that is useful outside of the workflow.

When building an interactive application, the `Rendering` type is commonly (but not always) a view model that will drive the UI layer.


### Workflows form a hierarchy (they may have children)

As they produce a `Rendering` value, it is common for workflows to delegate some portion of that work to a _child workflow_. This is also done via the `ComponentContext` that is passed into the `compose` method. In order to delegate to a child, the parent workflow instantiates the child within the `compose` method. The parent then calls `compose` on the context, with the child workflow as the single argument. The infrastructure will spin up the child workflow (including initializing its initial state) if this is the first time this child has been used, or, if the child was also used on the previous `compose` pass, the existing child will be updated. Either way, `compose` will ultimately be called on the child (by the Workflow infrastructure), and the resulting `Child.Rendering` value will be returned to the parent.

This allows a parent to return complex `Rendering` types (such as a view model representing the entire UI state of an application) without needing to model all of that complexity within a single workflow.


### Workflows can respond to UI events

The `WorkflowContext` that is passed into `compose` as the second parameter provides some useful tools to assist in creating the `Rendering` value. 

If a workflow is producing a view model, it is common to need an event handler to respond to UI events. The `WorkflowContext` has API to create an event handler that, when called, will advance the workflow by dispatching an action back to the workflow.


### Workflows can subscribe to external event sources

If a workflow needs to respond to some external event source (e.g. push notifications), the workflow can ask the context to listen to those events from within the `compose` method.


### Workflows can perform asynchronous tasks (Workers)

`Workers` are very similar in concept to child workflows. Unlike child workflows, however, workers do not have a `Rendering` type; they only exist to perform a single asynchronous task before sending an output event back up the tree to their parent.

A workflow can ask the infrastructure to await the result of a worker by handing that worker to the context within a call to the `compose` method.

### Workflows are advanced by `Action`s

Any time something happens that should advance a workflow – a UI event, a network response, a child's output event – actions are used to perform the update. For example, a workflow may respond to UI events by mapping those events into a type conforming to `WorkflowAction`. These types implement the logic to advance a workflow by:
- Advancing to a new state
- (Optionally) emitting an output event up the tree.


### Workflows can emit output events up the hierarchy to their parent

When a workflow is advanced by an action, an optional output event can be sent up the workflow hierarchy. This is the opportunity for a workflow to notify its parent that something has happened (and the parent's opportunity to respond to that event by dispatching its own action, continuing up the tree as long as output events are emitted).