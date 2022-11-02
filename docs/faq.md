# Frequently Asked Questions

## Why do we need another architecture?

We ask this question too! So we wrote a longer answer for it: ["Why Workflow?"](https://square.github.io/workflow/userguide/whyworkflow).

## How do I get involved and/or contribute?

- [Workflow is open source!](https://github.com/square/workflow)
- See our [CONTRIBUTING](https://github.com/square/workflow/blob/main/CONTRIBUTING.md) doc to get
  started.
- Stay tuned! We're considering hosting a public Slack channel for open source contributors.

## Isn't this basically React/Elm?

[React](https://reactjs.org/) and [the Elm architecture](https://guide.elm-lang.org/architecture/)
were both strong influences for this library. However both those libraries are written for
JavaScript. Workflows are written in and for both Kotlin and Swift, making use of features of those
languages, and with usability from those languages as a major design goal. There are some
architectural differences which we can see briefly in the following table:

|  | React | Elm | Workflow |
|---|---|---|---|
| **Modularity** | `Component` | `Module`s for code organization, but not 'composable' in the same way. | A `Workflow` is analogous to React's `Component` |
| **State** | Each `Component` has a `state` property that is read directly and updated via a `setState` method. | State is called `Model` in Elm. | `Workflow`s have an associated state type. The state can only be updated when the props change, or with a `WorkflowAction`. |
| **Views** | `Component`s have a `render` method that returns a tree of elements. | Elm applications have a `view` function that returns a tree of elements. | Since workflows are not tied to any particular UI view layer, they can have an arbitrary rendering type. The `render()` method returns this type. |
| **Injected Dependencies** | React allows parent components to pass "props" down to their children. | N/A | In Swift, `Workflow`s are often structs that need to be initialized with their dependencies and configuration data from their parent. In Kotlin, they have a separate type parameter (`PropsT`) that is always passed down from the parent. `Workflow` instances can also inject dependencies, and play nicely with dependency injection frameworks.
| **Composability** | `Component`s are composed of other `Component`s. | N/A | `Workflow`s can have children; they control their lifecycle and can choose to incorporate child renderings into their own. |
| **Event Handling** | DOM event listeners are hooked up to functions on the `Component`. | The `update` function takes a `Msg` to modify state based on events. | `action` can be sent to the `Sink` to update `State`. |

## How is this different than MvRx?

Besides being very Android and Rx specific, MvRx solves view modeling problems only
per screen. Workflow was mainly inspired by the need to manage and compose
navigation in apps with dozens or hundreds of screens.

## This seems clever. Can I stick with a traditional development approach?

Of course! Workflow was designed to make complex application architecture predictable and safe for
large development teams. We're confident that it brings benefits even to smaller projects, but there
is never only one right way to build software. We recommend to [follow good practices and use an
architecture that makes sense for your project](https://www.thoughtworks.com/insights/blog/write-quality-mobile-apps-any-architecture).
