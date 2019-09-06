# Step 6

_Workers running asynchronous work_

## Setup

To follow this tutorial:
- Open your terminal and run `bundle exec pod install` in the `swift/Samples/Tutorial` directory.
- Open `Tutorial.xcworkspace` and build the `Tutorial` Scheme.
- The unit tests will run from the default scheme when pressing `cmd+shift+u`.

Start from implementation of `Tutorial5` if you're skipping ahead. You can run this by updating the `AppDelegate` to import `Tutorial5` instead of `TutorialBase`.

# Workers

A Worker is defined in the [documentation](https://square.github.io/workflow/userguide/core-worker/) as:
"Worker is a protocol (in Swift) and interface (in Kotlin) that defines an asynchronous task that can be performed by a Workflow. Workers only emit outputs, they do not have a Rendering type. They are similar to child workflows with Void/Unit rendering types.

A workflow can ask the infrastructure to await the result of a worker by passing that worker to the `RenderContext.awaitResult(for: Worker)` method within a call to the render method. A workflow can handle outputs from a Worker."

We a `Worker` as our way to declaratively request asynchronous work to be performed, wrapping the imperative API for an asynchronous task.

# TODO List populated from Github issues

Our TODO list has a single placeholder item in it, but it would be more useful to fetch a list of TODOs from a server (and ideally sync them back). We'll add support for the former, using the Workflow projects list of issues as our set of TODO items.

## Fetching Issues from the Github API

The [Github API](https://developer.github.com/v3/issues/#list-issues-for-a-repository) provides an unauthenticated endpoint for fetching the issue from a repo.
