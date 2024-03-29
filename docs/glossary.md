# Glossary of Terms

## Reactive Programming

A style of programming where data or events are pushed to the logic processing them rather than having the logic pull the data and events from a source. A representation of program logic as a series of operations on a stream of data that is performed while a subscription to that stream is active.

## Unidirectional Data Flow

Data travels a single path from business logic to UI, and travels the entirety of that path in a single direction. Events travel a single path from UI to business logic and they travel the entirety of that path in a single direction. There are thus two sets of directed edges in the graph that are handled separately and neither set has any cycles or back edges on its own.

## Declarative Programming

A declarative program declares the state it wants the system to be in rather than how that is accomplished.

## Imperative Programming

An imperative program’s code is a series of statements that directly change a program's state as a result of certain events.

## State Machine

An abstraction that models a program’s logic as a graph of a set of states and the transitions between them (edges). See: <a href="https://en.wikipedia.org/wiki/Finite-state_machine">en.wikipedia.org/wiki/Finite-state_machine</a>

## Idempotent

A function whose side effects won’t be repeated with multiple invocations, the result is purely a function of the input. In other words, if called multiple times with the same input, the result is the same. For Workflows, the `render()` function must be idempotent, as the runtime offers no guarantees for how many times it may be called.

## Workflow Runtime

An event loop that executes a Workflow Tree. On each pass:

1. A Rendering is assembled by calling `render()` on each Node of the Workflow Tree with each parent Workflow given the option to incorporate the Renderings of its children into its own.

1. The event loop waits for an Action to be sent to the Sink.

1. This Action provides a (possibly updated) State for the Workflow that created it and possibly an Output.

1. Any Output emitted is processed in turn by an Action defined by the updated Workflow’s parent again possibly updating its State and emitting an Output cascading up the hierarchy.

1. A new `render()` pass is made against the entire Workflow Tree with the updated States.

We use the term Workflow Runtime to refer to the core code in the framework that executes this event loop, responding to Actions and invoking `render()`.

## Workflow (Instance)

An object that defines the transitions and side effects of a state machine as, effectively, two functions:

1. Providing the first state: (Props) -> State
1. Providing a rendering: (Props and State) -> (Rendering and Side Effect Invocations and Child Workflow Invocations)

The Child Workflow Invocations declared by the render function result in calls to the children’s `render()` functions in turn, allowing the parent render function to choose to incorporate child Rendering values into its own.

A Workflow is not itself a state machine, and ideally has no state of its own. It is rather a schema that identifies a particular type of state machine that can be started in `initialState()` by the Workflow Runtime, and advanced by repeated invocations of `render()`.

Note: there is significant fuzziness in using the term ‘Workflow’, as it can mean at times the class/struct that declares the Workflow behavior as well as the object representing the running Workflow Node. To understand the Runtime behavior, grasping this distinction is necessary and valuable. When using a Workflow, the formal distinction is less valuable than the mental model of how a Workflow will be run.

## Workflow (Node)

An active state machine whose behavior is defined by a Workflow Instance. This is the object that is held by the Workflow Runtime and whose state is updated (or “driven”) according to the behavior declared in the Workflow Instance. In Kotlin and Swift a Workflow Node is implemented with the private `WorkflowNode` class/struct.

## Workflow Lifecycle

Every Workflow or Side Effect Node has a lifecycle that is determined by its parent. In the case of the root Workflow, this lifecycle is determined by how long the host of the root chooses to use the stream of Renderings from the root Workflow. In the case of a non-root Workflow or Side Effect — that is, in the case of a Child — its lifecycle is determined as follows:

* Start: the first time its parent invokes the Child in the parent’s own `render()` pass.

* End: the first subsequent `render()` pass that does not invoke the Child.

Note that in between Start and End, the Workflow, or Side Effect is not “re-invoked” in the sense of starting again with each `render()` pass, but rather the originally invoked instance continues to run until a `render()` call is made without invoking it.

## Workflow Tree

The tree of Workflow Nodes sharing a root. Workflow Nodes can have children and form a hierarchy.

## Workflow Root

The root of a Workflow Tree. This is owned by a host which starts the Workflow Runtime with a particular Workflow instance.

## RenderContext

The object which provides access to the Workflow Runtime from a Workflow render method. Provides three services:

* a Sink for accepting WorkflowActions

* recursively rendering Workflow children

* executing Side Effects

## Render Pass

The portion of the Workflow Runtime event loop which traverses the Workflow tree, calling `render()` on each Workflow Node. When the RenderContext Sink receives an Action an Action Cascade occurs and at the completion of the Action Cascade the Render Pass occurs.

## Output Event

When a Child Workflow emits an Output value, this is an Output Event. Handlers are registered when a Child Workflow is invoked to transform the child’s Output values to Actions, which can advance the state of the parent.

## UI Event

Any occurrence in the UI of a program — e.g. click, drag, keypress — the listener for which has been connected to a callback in the Rendering of a Workflow. UI Event callbacks typically add Actions to the Sink, to advance the state of the Workflow.

## Action

A type associated with a particular Workflow (Instance) that is responsible for transforming a given State into a new State and optionally emitting an Output. Actions are sent to the Sink to be processed by the Workflow Runtime.

## Action Cascade

When an event occurs and the handler provides an Action, this Action may possibly produce an Output for the parent Workflow which in turn has its own handler provide an Action that may produce an Output and onwards up the Workflow Tree. This is an Action Cascade.

## Sink

The handle provided by the RenderContext to send Actions to the Workflow Runtime. These Actions are applied by the Workflow Runtime to advance a Workflow’s State, and optionally produce an Output to be processed by the handler its parent registered.

## Props

The set of input properties for a particular Workflow. This is the public state which is provided to a child Workflow by its parent, or to the root Workflow by its host.

* For Swift: The set of properties on the struct implementing the Workflow.

* For Kotlin: Parameter type `PropsT` in the Workflow signature.

In Kotlin there is a formal distinction between Props and other dependencies, typically provided as constructor parameters.

## State

The type of the internal state of a Workflow implementation.

## "Immutable" State

The State object itself is immutable, in other words, its property values cannot be changed.

What this means for Workflows is that the Workflow Runtime holds a canonical instance of the internal State of each Workflow. A Workflow’s state is “advanced” when that canonical instance is atomically replaced by one returned when an Action is invoked. State can only be mutated through WorkflowAction which will trigger a re-render. There are a number of benefits to keeping State immutable in this way:

* Reasoning about and debugging the Workflow is easier because, for any given State, there is a deterministic Rendering and the State cannot change except as a new parameter value to the `render()` method.

* This assists in making `render()` idempotent as the State will not be modified in the course of the execution of that function.

Note that this immutability can be enforced only by convention. It is possible to cheat, but that is strongly discouraged.

## Rendering

The externally available public representation of the state of a Workflow. It may include event handling functions. It is given a concrete type in the Workflow signature.

Note that this “Rendering” does not have to represent the UI of a program. The “Rendering” is simply the published state of the Workflow, and could simply be data. Often that data is used to render UI, but it can be used in other ways — for example, as the implementation of a service API.

## Output

The type of the object that can optionally be delivered to the Workflow’s parent or the host of the root Workflow by an Action.

## Child Workflow

A Workflow which has a parent. A parent may compose a child Workflow’s [Rendering](#rendering) into its own.

## Side Effect

From `render()`, `runningSideEffect()` can be called with a given key and a function that will be called once by the Workflow Runtime.

* For Swift, a Lifetime object is also passed to `runningSideEffect()` which has an `onEnded()` closure that can be used for cleanup.

* For Kotlin, a coroutine scope is used to execute the function so it can be `cancelled()` at cleanup time. Given that any property (including the Sink) could be captured by the closure of the Side Effect this is the basic building block that can be used to interact with asynchronous (and often imperative) Workflow Children.

## Worker

A Child Workflow that provides only output, with no [rendering](#rendering) — a pattern for doing asynchronous work in Workflows.

* For Kotlin, this is an actual Interface which provides a convenient way to specify asynchronous work that produces an Output and a handler for that Output which can provide an Action. There are Kotlin extensions to map Rx Observables and Kotlin Flows to create Worker implementations.

* For Swift, there are at least 3 different Worker types which are convenience wrappers around reactive APIs that facilitate performing work.

## View

A class or function managing a 2d box in a graphical user interface system, able to paint a defined region of the display and respond to user input events within its bounds. Views are arranged in a hierarchical tree, with parents able to lay out children and manage their painting and event handling.

Instances supported by Workflow are:

* For Kotlin:

    * Classic Android: `class android.view.View`
    * Android JetPack Compose: `@Composable fun Box()`

* For Swift: `class NSViewController`

## Screen

An interface / protocol identifying [Renderings](#rendering) that model a View. Workflow UI libraries can map a given Screen type to a View instance that can display a series of such Screens.

In Kotlin, `Screen` is a marker interface. Each type `S : Screen` is mapped by the Android UI library to a `ScreenViewFactory&lt;S>` that is able to:

* create instances of `android.view.View` or
* provide a `@Composable fun Content(S)` function to be called from a `Box {}` context.

Note that the Android UI support is able to interleave Screens bound to `View` or `@Composable` seamlessly.

In Swift, the `Screen` protocol defines a single function creating `ViewControllerDescription` instances, objects which create and update `ViewController` instances to display Screens of the corresponding type.

## Overlay (Kotlin only)

An interface identifying [Renderings](#rendering) that model a plane covering a base Screen, possibly hosting another Screen — “covering” in that they have a higher z-index, for visibility and event-handling.

In Kotlin, `Overlay` is a marker interface. Each type `O : Overlay` is mapped by the Android UI library to an `OverlayDialogFactory&lt;O>` able to create and update instances of `android.app.Dialog`

## Container Screen

A design pattern, describing a [Screen](#screen) type whose instances wrap one or more other Screens, commonly to either annotate those Screens or define the relationships between them.

Wrapping one Screen in another does not necessarily imply that the derived View hierarchy will change. It is common for the Kotlin `ScreenViewFactory` or Swift `ViewControllerDescription` bound to a Container Screen to delegate its construction and updating work to those of the wrapped Screens.

## Container View

A View able to host children that are driven by [Screen](#screen) renderings. A Container View is generally driven by Container Screens of a specific type — e.g., a BackStackContainer View that can display BackStackScreen values. The exception is a root Container View, which is able to display a series of Screen instances of any type.

* For Kotlin, the root Container Views are `WorkflowLayout : FrameLayout`, and `@Composable Workflow.renderAsState()`. Custom Container Views written to display custom Container Screens can use `WorkflowViewStub : FrameLayout` or `@Composable fun WorkflowRendering()` to display wrapped Screens.

* For Swift, the root Container View is `ContainerViewController`. Custom Container Views written to render custom Container Screens can be built as subclasses of `ScreenViewController`, and use `DescribedViewController` to display wrapped Screens.

## ViewEnvironment

A read-only key/value map passed from a [Container View](#container-view) down to its children at update time, similar in spirit to Swift UI `EnvironmentValues` and Jetpack `CompositionLocal`. Like them, the ViewEnvironment is primarily intended to allow parents to offer children hints about the context in which they are being displayed — for example, to allow a child to know if it is a member of a back stack, and so decide whether or not to display a Go Back button.

The ViewEnvironment also can be used judiciously as a service provider for UI-specific concerns, like image loaders — tread carefully.
