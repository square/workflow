# Common Patterns

There are a lot associated/generic types in workflow code – that doesn't mean you always need to use
all of them. Here are some common configurations we've seen.

## Stateless Workflows

Remember that workflow state is made up of public and private parts. When a workflow's state
consists entirely of public state (i.e. it's initializer arguments in Swift or `PropsT` in Kotlin),
it can ignore all the machinery for private state. In Swift, the`State` type can be `Void`, and in
`Kotlin` it can be `Unit` – such workflows are often referred to as "stateless", since they have no
state of their own.

## Props-less Workflows

Some workflows manage all of their state internally, and have no public state (aka props). In Swift,
this just means the workflow implementation has no parameters (although this is rare, see
_Injecting Dependencies_ below). In Kotlin, the `PropsT` type can be `Unit`. `RenderContext` has
convenience overloads of most of its functions to implicitly pass `Unit` for these workflows.

## Outputless Workflows

Workflows that only talk to their parent via their `Rendering`, and never emit any output, are
encouraged to indicate that by using the [bottom type](https://en.wikipedia.org/wiki/Bottom_type) as
their `Output` type. In addition to documenting the fact that the workflow will never output, using
the bottom type also lets the compiler enforce it – code that tries to emit outputs will not
compile. In Swift, the `Output` type is specified as [`Never`](https://nshipster.com/never/). In
Kotlin, use [`Nothing`](https://medium.com/@agrawalsuneet/the-nothing-type-kotlin-2e7df43b0111).

## Composite Workflows

Composition is a powerful tool for working with Workflows. A workflow can often accomplish a lot
simply by rendering various children. It may just combine the renderings of multiple children, or
use its props to determine which of a set of children to render. Such workflows can often be
stateless.

## One-and-done Workflows (RenderingT v. OutputT)

A common question is “why can’t I emit output from `initialState`,” or “what if my Workflow realizes it doesn’t actually need to run? The most efficient, and most expressive, way to handle this is to use an optional or conditional `Rendering` type, and an `Output` of [`Never`](https://nshipster.com/never/)/[`Nothing`](https://medium.com/@agrawalsuneet/the-nothing-type-kotlin-2e7df43b0111).

Imagine a `PromptForPermissionMaybeWorkflow`, that renders a UI to get a passcode, but only if that permission has not already been granted. If you make its `RenderingT` nullable (e.g. `Screen?`), it can return `null` to indicate that its job is done. Its callers will be synchronously informed that the coast is clear, and can immediately render what they actually care about.

Another variation of this pattern is to use a sealed class / enum type for `Rendering`, with a `Working` type that implements `Screen`, and a unviewable `Finished` type that carries the work product.

A good rule of thumb for choosing between using `Rendering` or `Output` is to remember that `Output` is event-like, and is always asynchronous. A parent waiting for an output must be given something to render in the meantime. Using `Rendering` is a great idiom for a one-and-done workflow tasked with providing a single product, especially one that might be available instantly.

## Props values v. Injected Dependencies

[Dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) is a technique for making
code less coupled and more testable. In short, it's better for classes/structs to accept their
dependencies when they're created instead of hard-coding them. Workflows typically have dependencies
like specific Workers they need to perform some tasks, child workflows to delegate rendering to, or
helpers for things like network requests, formatting and logging.

### Swift

A Swift workflow typically receives its dependencies as initializer arguments, just like its input
values, and is normally instantiated anew by its parent in each call to the parent’s render method.
The [factory pattern](https://en.wikipedia.org/wiki/Factory_method_pattern) can be employed to keep
knowledge of children’s implementation details from leaking into their parents.

### Kotlin

Kotlin workflows make a more formal distinction between dependencies and props, via the `PropsT`
parameter type on the Kotlin `Workflow` interface. Dependencies (e.g. a network service) are
typically provided as constructor parameters, while props values (e.g. a record locator) are
provided by the parent as an argument to the `RenderContext.renderChild` method.  This works
seamlessly with DI libraries like [Dagger](https://dagger.dev/).

The careful reader will note that this is technically storing "state" in the workflow instance –
something that is generally discouraged. However, since this "state" is never changed, we can make
an exception for this case. If a workflow has properties, they should _only_ be used to store
injected dependencies or dependencies derived from injected ones (e.g. `Worker`s created from
`Observable`s).

!!! info Swift vs Kotlin
    This difference between Swift and Kotlin practices is a side effect of Kotlin’s lack of a
    parallel to Swift’s `Self` type. Kotlin has no practical way to provide a method like Swift’s
    `Workflow.workflowDidChange`, which accepts a strongly typed reference to the instance from the
    previous run of a parent’s `Render` method. Kotlin’s alternative,
    `StatefulWorkflow.onPropsChanged`, requires the extra `PropsT` type parameter.
