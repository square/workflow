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

## Skippable Workflows

* Why can’t I emit output from `initialState`?
* Why can’t I emit output from `render`?
* Why can't I change states during `render`?
* What if while rendering my Workflow realizes it doesn’t actually need to run?

The best pattern we've found for all of these situations is a "skippable workflow".
The idea is that a workflow that performs work that may not actually be necessary
provides an extra function for parents to call.
The parent can call this during the upstream action
where it's deciding whether or not to enter the state
in which the child would be run.

Perhaps it's a simple boolean function indicating whether or not the child actually has any work to do:

```kotlin
state = if (stepTwoWorkflow.isThisNecessary()) RunStepTwo else RunStepThree
```

Perhaps it can return the usual output, or null to indicate that async work is required:

```kotlin
state = stepTwoWorkflow.instantResponse(stepTwoInput)?.let { RunStepThree(it) }
  ?: RunStepTwo
```

A particularly nice aspect of this approach is how it keeps
the knowledge encapsulated by the skippable workflow from leaking.

Any hacks you might resort during `initialState` or `render`
to make it seem like a workflow is short circuiting are big smells.
They are guaranteed to cause performance problems that compound,
and add needless complexity.

## Optional renderings (using RenderT instead of OutputT)

Besides the [skippable workflow](#skippable-workflows) pattern described above,
another way a child can communicate to its parent that it has no work to do
is with a nullable or otherwise un-renderable rendering type.

Imagine a `PromptForPermissionMaybeWorkflow`, that renders a UI to get a passcode, but only if that permission has not already been granted. If you make its `RenderingT` nullable (e.g. `Screen?`), it can return `null` to indicate that its job is done. Its callers will be synchronously informed that the coast is clear, and can immediately render what they actually care about.

Another variation of this pattern is to use a sealed class / enum type for `Rendering`, with a `Working` type that implements `Screen`, and a unviewable `Finished` type that carries the work product.

Either way, it's a good idea to use an an output type of
[`Never`](https://nshipster.com/never/)/[`Nothing`](https://medium.com/@agrawalsuneet/the-nothing-type-kotlin-2e7df43b0111)
in a case like this, to keep it crystal clear how the child reports to the parent.

This pattern requires that the parent will not need to change state or emit output
based on the returned rendering type.
The parent must be able to choose some other child to run or something else to render immediately.
If that is not practical, go [skippable](#skippable-workflows) instead.

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
