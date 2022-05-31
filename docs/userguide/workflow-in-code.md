# Coding a Workflow

In code, `Workflow` is a Swift protocol or Kotlin interface with State, Rendering and Output parameter types.
The Kotlin interface also defines a Props type.
In Swift, props are implicit as properties of the struct implementing Workflow.

=== "Swift"
    ```Swift
    public protocol Workflow: AnyWorkflowConvertible {

        associatedtype State

        associatedtype Output = Never

        associatedtype Rendering

        func makeInitialState() -> State

        func workflowDidChange(from previousWorkflow: Self, state: inout State)

        func render(state: State, context: RenderContext<Self>) -> Rendering

    }

    ```

=== "Kotlin"
    ```Kotlin
    abstract class StatefulWorkflow<in PropsT, StateT, out OutputT : Any, out RenderingT> :
        Workflow<PropsT, OutputT, RenderingT> {

      abstract fun initialState(
        props: PropsT,
        initialSnapshot: Snapshot?
      ): StateT

      open fun onPropsChanged(
        old: PropsT,
        new: PropsT,
        state: StateT
      ): StateT = state

      abstract fun render(
        props: PropsT,
        state: StateT,
        context: RenderContext<StateT, OutputT>
      ): RenderingT

      abstract fun snapshotState(state: StateT): Snapshot
    }
    ```

??? faq "Swift: What is `AnyWorkflowConvertible`?"
    When a protocol has an associated `Self` type, Swift requires the use of a [type-erasing wrapper](https://medium.com/swiftworld/swift-world-type-erasure-5b720bc0318a)
    to store references to instances of that protocol.
    [`AnyWorkflow`](/workflow/swift/api/Workflow/Structs/AnyWorkflow.html) is such a wrapper for
    `Workflow`. [`AnyWorkflowConvertible`](/workflow/swift/api/Workflow/Protocols/AnyWorkflowConvertible.html)
    is a protocol with a single method that returns an `AnyWorkflow`. It is useful as a base type
    because it allows instances of `Workflow` to be used directly by any code that requires the
    type-erased `AnyWorkflow`.

??? faq "Kotlin: `StatefulWorkflow` vs `Workflow`"
    It is a common practice in Kotlin to divide types into two parts: an interface for public API,
    and a class for private implementation. The Workflow library defines a [`Workflow`](/workflow/kotlin/api/gfmCollector/workflow/com.squareup.workflow1/-workflow/)
    interface, which should be used as the type of properties and parameters by code that needs to
    refer to a particular `Workflow` interface. The `Workflow` interface contains a single method,
    which simply returns a `StatefulWorkflow` – a `Workflow` can be described as “anything that can
    be expressed as a `StatefulWorkflow`.”

    The library also defines two abstract classes which define the contract for workflows and should
    be subclassed to implement your workflows:

    - [**`StatefulWorkflow`**](/workflow/kotlin/api/gfmCollector/workflow/com.squareup.workflow1/-stateful-workflow/)
      should be subclassed to implement Workflows that have [private state](#private-state).
    - [**`StatelessWorkflow`**](/workflow/kotlin/api/gfmCollector/workflow/com.squareup.workflow1/-stateless-workflow/)
      should be subclassed to implement Workflows that _don't_ have any private state. See [Stateless Workflows](#stateless-workflows).

Workflows have several responsibilities:

## Workflows have state

Once a Workflow has been started, it always operates in the context of some state. This state is
divided into two parts: private state, which only the Workflow implementation itself knows about,
which is defined by the `State` type, and properties (or "props"), which is passed to the Workflow
from its parent (more on hierarchical workflows below).

### Private state

Every Workflow implementation defines a `State` type to maintain any necessary state while the
workflow is running.

For example, a tic-tac-toe game might have a state like this:

=== "Swift"
    ```Swift
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

=== "Kotlin"
    ```Kotlin
    data class State(
      // 3 rows * 3 columns = 9 spaces
      val spaces: List<Space> = List(9) { Unfilled },
      val currentTurn: Player = X
    ) {

      enum class Player {
        X, O
      }

      sealed class Space {
        object Unfilled : Space()
        data class Filled(val player: Player) : Space()
      }
    }
    ```

When the workflow is first started, it is queried for an initial state value. From that point
forward, the workflow may advance to a new state as the result of events occurring from various
sources (which will be covered below).

!!! info "Stateless Workflows"
    If a workflow does not have any private state, it is often referred to as a
    "stateless workflow". A stateless Workflow is simply a Workflow that has a `Void` or `Unit`
    `State` type. See more [below](#stateless-workflows).

### Public Props

Every Workflow implementation also defines data that is passed into it. The Workflow is not able to
modify this state itself, but it may change between render passes. This public state is called
`Props`.

In Swift, the props are simply defined as properties of the struct implementing Workflow itself. In
Kotlin, the `Workflow` interface defines a separate `PropsT` type parameter. (This additional type
parameter is necessary due to Kotlin’s lack of the `Self` type that Swift workflow’s
`workflowDidChange` method relies upon.)

=== "Swift"
    ```Swift
    TK
    ```

=== "Kotlin"
    ```Kotlin
    data class Props(
      val playerXName: String
      val playerOName: String
    )
    ```

## Workflows are advanced by `WorkflowAction`s

Any time something happens that should advance a workflow – a UI event, a network response, a
child's output event – actions are used to perform the update. For example, a workflow may respond
to UI events by mapping those events into a type conforming to/implementing `WorkflowAction`. These
types implement the logic to advance a workflow by:

- Advancing to a new state
- (Optionally) emitting an output event up the tree.

`WorkflowAction`s are typically defined as enums with associated types (Swift) or sealed classes
(Kotlin), and can include data from the event – for example, the ID of the item in the list that was
clicked.

Side effects such as logging button clicks to an analytics framework are also typically performed in
actions.

If you're familiar with React/Redux, `WorkflowAction`s are essentially reducers.

## Workflows can emit output events up the hierarchy to their parent

When a workflow is advanced by an action, an optional output event can be sent up the workflow
hierarchy. This is the opportunity for a workflow to notify its parent that something has happened
(and the parent's opportunity to respond to that event by dispatching its own action, continuing up
the tree as long as output events are emitted).

## Workflows produce an external representation of their state via `Rendering`<a id="rendering"></a>

Immediately after starting up, or after a state transition occurs, a workflow will have its `render`
method called. This method is responsible for creating and returning a value of type `Rendering`.
You can think of `Rendering` as the "external published state" of the workflow, and the `render`
function as a map of (`Props` + `State` + childrens' `Rendering`s) -> `Rendering`. While a
workflow's internal state may contain more detailed or comprehensive state, the `Rendering`
(external state) is a type that is useful outside of the workflow. Because a workflow’s render
method may be called by infrastructure for a variety of reasons, it’s important to not perform side
effects when rendering — render methods must be idempotent. Event-based side effects should use
Actions and state-based side effects should use Workers.

When building an interactive application, the `Rendering` type is commonly (but not always) a view
model that will drive the UI layer.

## Workflows can respond to UI events

The `RenderContext` that is passed into `render` as the last parameter provides some useful tools to
assist in creating the `Rendering` value.

If a workflow is producing a view model, it is common to need an event handler to respond to UI
events. The `RenderContext` has API to create an event handler, called a `Sink`, that when called
will advance the workflow by dispatching an action back to the workflow (for more on actions, see
[below](#workflows-are-advanced-by-actions)).

=== "Swift"
    ```Swift
    func render(state: State, context: RenderContext<DemoWorkflow>) -> DemoScreen {
        // Create a sink of our Action type so we can send actions back to the workflow.
        let sink = context.makeSink(of: Action.self)

        return DemoScreen(
            title: "A nice title",
            onTap: { sink.send(Action.refreshButtonTapped) }
    }
    ```

=== "Kotlin"
    ```Kotlin
    TK
    ```

## Workflows form a hierarchy (they may have children)

As they produce a `Rendering` value, it is common for workflows to delegate some portion of that
work to a _child workflow_. This is done via the `RenderContext` that is passed into the `render`
method. In order to delegate to a child, the parent calls `renderChild` on the context, with the
child workflow as the single argument. The infrastructure will spin up the child workflow (including
initializing its initial state) if this is the first time this child has been used, or, if the child
was also used on the previous `render` pass, the existing child will be updated. Either way,
`render` will immediately be called on the child (by the Workflow infrastructure), and the resulting
child's `Rendering` value will be returned to the parent.

This allows a parent to return complex `Rendering` types (such as a view model representing the
entire UI state of an application) without needing to model all of that complexity within a single
workflow.

!!! info "Workflow Identity"
    The Workflow infrastructure automatically detects the first time and the last subsequent time
    you've asked to render a child workflow, and will automatically initialize the child and clean
    it up. In both Swift and Kotlin, this is done using the workflow's concrete type. Both languages
    use reflection to do this comparison (e.g. in Kotlin, the workflows' `KClass`es are compared).

    It is an error to render workflows of the same type more than once in the same render pass.
    Since type is used for workflow identity, the child rendering APIs take an optional string key
    to differentiate between multiple child workflows of the same type.

## Workflows can subscribe to external event sources

If a workflow needs to respond to some external event source (e.g. push notifications), the workflow
can ask the context to listen to those events from within the `render` method.

!!! info "Swift vs Kotlin"
    In the Swift library, there is a special API for subscribing to hot streams (`Signal` in
    ReactiveSwift). The Kotlin library does not have any special API for subscribing to hot streams
    (channels), though it does have extension methods to convert [`ReceiveChannel`s](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/),
    and RxJava `Flowable`s and `Observables`, to [`Worker`s](#worker). The reason for this
    discrepancy is simply that we don't have any uses of channels yet in production, and so we've
    decided to keep the API simpler. If we start using channels in the future, it may make sense to
    make subscribing to them a first-class API like in Swift.

## Workflows can perform asynchronous tasks (Workers)

`Workers` are very similar in concept to child workflows. Unlike child workflows, however, workers
do not have a `Rendering` type; they only exist to perform a single asynchronous task before sending
zero or more output events back up the tree to their parent.

For more information about workers, see the [Worker](#worker) section below.

## Workflows can be saved to and restored from a snapshot (Kotlin only)

On every render pass, each workflow is asked to create a "snapshot" of its state – a lazily-produced
serialization of the workflow's `State` as a binary blob. These `Snapshot`s are aggregated into a
single `Snapshot` for the entire workflow tree and emitted along with the root workflow's
`Rendering`. When the workflow runtime is started, it can be passed an optional `Snapshot` to
restore the tree from. When non-null, the root workflow's snapshot is extracted and passed to the
root workflow's `initialState`. The workflow can choose to either ignore the snapshot or use it to
restore its `State`. On the first render pass, if the root workflow renders any children that were
also being rendered when the snapshot was taken, those children's snapshots are also extracted from
the aggregate and used to initialize their states.

!!! faq "Why don't Swift Workflows support snapshotting?"
    Snapshotting was built into Kotlin workflows specifically to support Android's app lifecycle,
    which requires apps to serialize their current state before being backgrounded so that they can
    be restored in case the system needs to kill the hosting process. iOS apps don't have this
    requirement, so the Swift library doesn't need to support it.
