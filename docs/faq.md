# Frequently Asked Questions

## Isn't this basically React/Elm?

[React](https://reactjs.org/) and [the Elm architecture](https://guide.elm-lang.org/architecture/)
were both strong influences for this library. However both those libraries are written for
JavaScript. Workflows are written in and for both Kotlin and Swift, making use of features of those
languages, and with usability from those languages as a major design goal. There are also a few
architectural differences:

|  | React | Elm | Workflow |
|---|---|---|---|
| **Modularity** | `Component` | TK | `Workflow` is analogous to React's `Component` |
| **State** | Each `Component` has a `state` property that is read directly and updated via a `setState` method. | State is called `Model` in Elm. | `Workflow`s have an associated state type. The state can only be updated when the props change, or with a `WorkflowAction`. |
| **Views** | `Component`s have a `render` method that returns a tree of elements. | Elm applications have a `view` function that returns a tree of elements. | Since workflows are not tied to any particular UI view layer, they can have an arbitrary rendering type. The `render()` method returns this type. |
| **Dependencies** | React allows parent components to pass "props" down to their children. | TK | In Swift, `Workflow`s are often structs that need to be initialized with their dependencies and configuration data from their parent. In Kotlin, they have a separate type parameter (`PropsT`) that is always passed down from the parent. `Workflow` instances can also inject dependencies, and play nicely with dependency injection frameworks.
| **Composability** | TK | TK | TK |
| **Event Handling** | TK | TK | TK |

## How is this different than MvRx?

Besides being very Android and Rx specific, MvRx solves view modeling problems only
per screen. Workflow was mainly inspired by the need to manage and compose
navigation in apps with dozens or hundreds of screens.

## How do I get involved and/or contribute?

- [Workflow is open source!](https://github.com/square/workflow)
- See our [CONTRIBUTING](https://github.com/square/workflow/blob/master/CONTRIBUTING.md) doc to get
  started.
- Stay tuned! We're considering hosting a public Slack channel for open source contributors.

## This seems clever. Can I stick with a traditional development approach?

Of course! Workflow was designed to make complex application architecture predictable and safe for
large development teams. We're confident that it brings benefits even to smaller projects, but there
is never only one right way to build software. We recommend to [follow good practices and use an
architecture that makes sense for your project](https://www.thoughtworks.com/insights/blog/write-quality-mobile-apps-any-architecture).

## Why do we need another architecture?

Architectural patterns with weak access controls and heavy use of shared mutable state make it
incredibly difficult to fully understand the behavior of the code that we are writing. This quickly
devolves into an arms race as the codebase grows: if every feature or component in the codebase
might change anything at any time, bug fixes turn into a really sad game of whack-a-mole.

We have seen this pattern occur repeatedly in traditional mobile applications using patterns like
MVC.

Workflow defines strong boundaries and contracts between separate parts of the application to ensure
that our code remains predictable and maintainable as the size and complexity of the codebase grows.
