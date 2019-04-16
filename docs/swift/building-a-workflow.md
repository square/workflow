# Building a Workflow


## Introduction

A simple workflow looks something like this:

```swift
struct DemoWorkflow: Workflow {

    var name: String

    init(name: String) {
        self.name = name
    }

}

extension DemoWorkflow {

    struct State {}

    func makeInitialState() -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {

    }
    
    func compose(state: State, context: WorkflowContext<DemoWorkflow>) -> String {
        return "Hello, \(name)"
    }

}
```

A type conforming to `Workflow` represents a single node in the workflow tree. It should contain any values that must be provided by its parent (who is generally responsible for creating child workflows).

Configuration parameters, strings, network services… If your workflow needs access to a value or object that it cannot create itself, they should be passed into the workflow's initializer.

Every workflow defines its own `State` type to contain any data that should persist through subsequent compose passes.

## Compose

Workflows are only useful when they render a value for use by their parent (or, if they are the root workflow, for display). This type is very commonly a view model, or `Screen`. The `compose(state:context:)` method has a couple of parameters, so we’ll work through them one by one.

```swift
func compose(state: State, context: WorkflowContext<DemoWorkflow>) -> Rendering
```

### `state`

Contains a value of type `State` to provide access to the current state. Any time the state of workflow changes, `compose` is called again to take into account the change in state.

### `context`

The workflow context:
- provides a way for a workflow to defer to nested (child) workflows to generate some or all of its rendered output. We’ll walk through that process later on when we cover composition.
- allows a workflow to request the execution of asynchronous tasks (`Worker`s)
- generates event handlers for use in constructing view models.

In order for us to see the anything in our app, we'll need to return a `Screen` that can be turned into a view controller:

```swift
    func compose(state: State, context: WorkflowContext<DemoWorkflow>) -> Screen {
        return DemoScreen(title: "A nice title")
    }
```

## Actions, or “Things that advance a workflow”

So far we have only covered workflows that perform simple tasks like generate strings or simple screens with no actions. If our workflows take on a complicated roles like generating view models, however, they will inevitably be required to handle events of some kind – some from UI events such as button taps, others from infrastructure events such as network responses.

In conventional UIKit code, it is common to deal with each of those event types differently. The common pattern is to implement a method like `handleButtonTap(sender:)`. Workflows are more strict about events, however. Workflows require that all events be expressed as "Workflow Actions."

These actions should be thought of as the entry point to your workflow. If any action of any kind happens (that your workflow cares about), it should be modeled as an action.

```swift
struct DemoWorkflow: Workflow {
    /// ...
}

enum Action: WorkflowAction {

    typealias WorkflowType = DemoWorkflow

    case refreshButtonTapped /// UI event
    case refreshRequestFinished(RefreshResponse) /// Network event

    func apply(toState state: inout DemoWorkflow.State) -> DemoWorkflow.Output? {
        /// ...
    }
}
```

## The Update Cycle

Every time a new action is received, it is applied to the current state of the workflow. If your workflow does more than simply render values, the action's `apply` is the method where the logic lives.

There are two things that the `apply(toState:)` method is responsible for:
- Transitioning state
- (Optionally) emitting an output event

Note that the `compose(state:context:)` method is called after every state change, so you can be sure that any state changes will be reflected.

Since we have a way of expressing an event from our UI, we can now use the callback on our view model to send that event back to the workflow:

```swift
func compose(state: State, context: WorkflowContext<DemoWorkflow>) -> Screen {
    return DemoScreen(
        title: "A nice title",
        onTap: context.makeEventHandler(action: Action.refreshButtonTapped)
}
```

## State

Some workflows do not need state at all – they simply render values based on the values they were initialized with. But for more complicated workflows, state management is critical. For example, a multi-screen flow only functions if we are able to define all of the possible steps (model the state), remember which one we are currently on (persist state), and move to other steps in the future (transition state).

To define your workflow's state, simply implement the associatedtype `State` via an enum or struct.

```swift
struct WelcomeFlowWorkflow: Workflow {

    enum State {
        case splashScreen
        case loginFlow
        case signupFlow
    }

    enum Action: WorkflowAction {
        case back
        /// ...
    }

    /// ...
}
```

*__Note:__ Workflows (and their `State`) should always be implemented through value types (structs and enums) due to the way the framework handles state changes. This means that you can never capture references to `self`, but the consistent flow of data pays dividends – try this architecture for a while and we are confident that you will see the benefits.*

## Workers

Workers provide a declarative interface to units of asyncronous work (such as network requests).

## Output Events

The last role of the update cycle is to emit output events. As workflows form a hierarchy, it is common for children to send events up the tree. This may happen when a child workflow finishes or cancels, for example.

Workflows can define an output type, which may then be returned by Actions.


## Composition

Composition is the primary tool that we can use to manage complexity in a growing application. Workflows should always be kept small enough to be understandable – less than 150 lines is a good target. By composing together multiple workflows, complex problems can be broken down into individual pieces that can be quickly understood by other developers (including future you).

The context provided to the `compose(state:context:)` method defines the API through which composition is made possible.

### The Workflow Context

The useful role of children is ultimately to provide rendered values (typically screen models) via their `compose(state:context:)` implementation. To obtain that value from a child workflow, the `render(workflow:key:outputMap:)` method is invoked on the render context.

When a workflow is passed into the context’s render method, the context will do the following:
- Check if the child workflow is new or existing:
 - If a workflow with the same type was used during the last render pass, the existing child workflow will be updated with the new workflow.
 - Otherwise, a new child workflow node will be initialized.
- The child workflow's `compose(state:context:)` method is called.
- The rendered value is returned.

In practice, this looks something like this:

```swift
struct ParentWorkflow: Workflow {

    func compose(state: State, context: WorkflowContext<ParentWorkflow>) -> String {
        let childWorkflow = ChildWorkflow(text: "Hello, World")
        return context.render(childWorkflow) /// returns "dlroW ,olleH"
    }

}

struct ChildWorkflow: Workflow {

    var text: String

    // ...

    func compose(state: State, context: WorkflowContext<ChildWorkflow>) -> String {
        return String(text.reversed())
    }
}
```
