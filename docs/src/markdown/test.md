---
title: Components (iOS)
---

# Components (iOS)

**Components are the building blocks that are used to build Workflows on iOS.**

----

## Introduction

Components provide a way to build complex applications out of small, isolated pieces with a predictable data flow and a consistent API contract. They are conceptually similar to components in architecture patterns such as React (though they are fully native and type-safe).

----

*__Note:__ One important difference between these components and those found in web frontend frameworks comes from the vast differences between the DOM and UIKit. The DOM is already declarative (meaning that we can always reason about the element tree in a web page). UIKit is not – it very much relies on a procedural programming model where transitions are performed by imperative methods like `push`, `fadeOut`, etc. For this reason, our components do not ever refer directly to views. They are instead responsible for rendering view models. This view model can then be used to update the UI (though that task is outside the scope of this framework).*

----


The most simple way to think about a component is that it receives input (via its `Properties` value), and it generates a rendered value from its `render(properties:sink:context:)` method (via the `Rendering` type that is defined by each component).

A basic component looks something like this:

```swift
struct Welcome: Component {

    struct Properties {
        var name: String
    }
    
    func render(properties: Properties, sink: Sink<Never>, context: RenderContext<Never>) -> String {
        Return "Hello, \(properties.name)"
    }

}
```

## Properties

Every component defines its own `Properties` type to contain any input  that is required in order for the component to function. 

Configuration parameters, strings, network services… If your component needs access to a value or object that it cannot create internally, properties are the transport mechanism through which those values can be passed in from an external source.

Components are never initialized directly, so their properties must contain every single piece of data that they require in order to function.

## Rendering

Components are only useful when they render a value to be consumed by other parts of the application – commonly to view model types. The `render(properties:sink:context:)` method has a few parameters, so we’ll work through them one by one.

```swift
func render(properties: Properties, sink: Sink<Event>, context: RenderContext<Event>) -> Rendering
```

### `properties`

Contains a value of type `Properties` to provide access to any input needed by this component.

### `sink`

Sinks are simple types that are analogous to a callback – they can have values sent into them. This sink acts as an input for UI events. If the component is generating a view model that includes event callbacks, the sink can be captured and used to deliver events back to the component. We’ll cover the role of a component’s events later on.

### `renderContext`

The render context provides a way for a component to defer to nested (child) components to generate some or all of its rendered output. We’ll walk through that process later on when we cover composition.

## Composition

Composition is the primary tool that we can use to manage complexity in a growing application. Components should always be kept small enough to be understandable – less than 150 lines is a good target. By composing together multiple components, complex problems can be broken down into individual pieces that can be quickly understood by other developers (including future you).

The render context provided to the `render(properties:sink:context:)` method defines the API through which composition is made possible.

### `ComponentDescription`

Component description describes  a component. Which is to say, it refers to a component, but it does not contain a component.

As previously discussed, properties comprehensively define the input to a component. This means that we can refer to a component via a tuple of two values:
- The type of a component implementation
- A properties value for that component type

That is exactly the information contained within ComponentDescription. It has a few additional APIs as well, including methods to map and transform various properties of the component that it represents.

### The Render Context

While a component description provide a way to refer to a component, the render context allows us to turn that description into an actual child component.

When a component description is passed into the context’s render method, the context will do the following:
- Check if the child component is new or existing:
 - If a component description with the same underlying component type was used during the last render pass, the existing child component will be updated with the new properties value contained in the component description.
 - Otherwise, a new child component will be initialized with the properties contained in the component description.
- The child component’s `render(properties:sink:context:)` method is called.
- The rendered value is returned.

In practice, this looks something like this:

```swift
struct ParentComponent {

    func render(properties: Void, sink: Sink<Never>, context: RenderContext<Never>) -> String {
        let description = ChildComponent.description(properties: "Hello, World")
        return context.render(description) /// returns "dlroW ,olleH"
    }

}

struct ChildComponent {
    func render(properties: String, sink: Sink<Never>, context: RenderContext<Never>) -> String {
        Return String(properties.reversed())
    }
}
```


## Events, or “Things That Happen Later”

So far we have only covered components that perform simple tasks like generate strings. If our components take on my complicated roles like generating view models, however, they will inevitably be required to handle events of some kind – some from UI events such as button taps, others from infrastructure events such as network responses.

In conventional UIKit code, it is common to deal with each of those event types differently. The common pattern is to implement a method like `handleButtonTap(sender:)`. Components are more strict about events, however. Components require that all events be expressed through a single `Event` type.

This event type should be thought of as the entry point to your component. If any action of any kind happens (that your component cares about), it should be modeled as an event.

```swift
struct DemoComponent: Component {
    /// ...

    enum Event {
        case refreshButtonTapped /// UI event
        case refreshRequestFinished(RefreshResponse) /// Network event
    }

    /// ...
}
```

## The Update Cycle

Every time a new event is received, it is handled by a component’s `update(event:context:)` method. If your component does more than simply render values, this is the method where the logic lives.

```swift
struct DemoComponent: Component {
    /// ...

    enum Event {
        case refreshButtonTapped /// UI event
        case refreshRequestFinished(RefreshResponse) /// Network event
    }

    mutating func update(event: Event, context: inout UpdateContext<DemoComponent> {
        switch event {
            /// do things
        }
    }

    /// ...
}
```

There are three things that the `update(event:context:)` method is responsible for:
- Transitioning state
- Performing commands
- Emitting output events

Note that the `render(properties:sink:context:)` method is called after every update, so you can be sure that any state changes will be reflected.

## State

Some components do not need state at all – they simple render values based on their properties. But for more complicated components, state management is critical. For example, a multi-screen flow only functions if we are able to define all of the possible steps (model the state), remember which one we are currently on (persist state), and move to other steps in the future (transition state).

Modeling state in a component is easy: components extend the StateMachine protocol. To define your component’s state, simply implement the component via an enum or struct with appropriate properties… the component is the state.

```swift
enum WelcomeFlowComponent: Component {
    case splashScreen
    case loginFlow
    case signupFlow

    enum Event {
        case back
        /// ...
    }

    mutating func update(event: Event, context: inout UpdateContext<WelcomeFlowComponent>) {
        switch event {
        case .back:
            switch self {
            case .loginFlow:
                self = .splashScreen
            /// Handle other cases 
            }
        }
    }
    

    /// ...
}
```

The update method is mutating, so it’s easy to perform state transitions in response to an event.

*__Note:__ Components should always be implemented through value types (structs and enums) due to the way the framework handles state changes. This means that you can never capture references to `self`, but the consistent flow of data pays dividends – try this architecture for a while and we are confident that you will see the benefits.*

## Commands

Commands provide a mechanism by which components can perform side effects. Side effects include things like network requests, CRUD operations, etc. Anything that requires an external method to be performed that changes things in another piece of the application (or anywhere outside of our own state) should be considered a side effect.

Components can define a command type to model the various types of side effects that they can perform.

```swift
struct DemoComponent: Component {
    /// ...

    enum Command {
        case performLogin(email: String, password: String)
    }

    mutating func update(event: Event, context: inout UpdateContext<DemoComponent>) {
        switch event {
        case .loginTapped:
            context.send(command: .performLogin(email: "me@internet.com", password: "asdf"))
        }
    }

    /// ...
}
```

Those commands are then added to the update context during a call to `update(event:context:)`.

If a component defines a command type, it must also provide an implementation of `run(command:properties)`:

```swift
struct DemoComponent: Component {
    /// ...

    enum Command {
        case performLogin(email: String, password: String)
    }

    Static func run(command: Command, properties: Properties) -> SignalProducer<Event, NoError> {
        switch command {
        case .performLogin(let email, let password):
            return properties.networkService.doLogin(email, password)
        }
    }

    /// ...
}
```

The `run(command:properties:)` command is responsible for performing actual side effects for a given command, then returning a signal producer through which the result of that operation can be fed back into the component.

## Output Events

The last role of the update cycle is to emit output events. As components form a hierarchy, it is common for children to send events up the tree. This may happen when a child component finishes or cancels, for example.

Components can define an output type, which is then set on the update context during a call to update:

```swift
struct DemoComponent: Component {
    /// ...

    enum Output {
        case finished
        case cancelled
    }

    mutating func update(event: Event, context: inout UpdateContext<DemoComponent>) {
        switch event {
        case .finishButtonTapped:
            context.output = .finished
        case .cancelButtonTapped:
            context.output = .cancelled
        }
    }

    /// ...
}
```
