# Swift

!!! tip
    For a comprehensive tutorial with code that you can build and follow along with, see the
    [Tutorials](https://github.com/square/workflow/tree/master/swift/Samples/Tutorial#tutorial) in
    the repo.

    This section will be restructured soon to incorporate that and Kotlin tutorials.

The Workflow infrastructure is split into several modules.

## `Workflow`

The `Workflow` library contains the core types that are used to implement state-driven workflows,
including the `Workflow` protocol and related indrastructure.

## `WorkflowUI`

Contains the basic infrastructure required to build a Workflow-based application that uses `UIKit`.

---

Workflow for iOS makes extensive use of [ReactiveSwift](https://github.com/ReactiveCocoa/ReactiveSwift).
If you are new to reactive programming, you may want to familiarize yourself with some of the
basics. Workflow takes care of a lot of the reactive plumbing in a typical application, but you will
have a better time if you understand what the framework is doing.

* [Core Reactive Primitives](https://github.com/ReactiveCocoa/ReactiveSwift/blob/master/Documentation/ReactivePrimitives.md)
* [Basic Operators](https://github.com/ReactiveCocoa/ReactiveSwift/blob/master/Documentation/BasicOperators.md)
* [How does ReactiveSwift relate to RxSwift?](https://github.com/ReactiveCocoa/ReactiveSwift/blob/master/Documentation/RxComparison.md)

## Next Steps

* [Tooling](tooling.md)
* [Adding Workflow to a Project](adding-workflow-to-a-project.md)
* [Building a Workflow](building-a-workflow.md)
* [Building a View Controller from a Screen](building-a-view-controller-from-screen.md)
* [Using a Workflow to Show UI](using-a-workflow-for-ui.md)
