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

We use a `Worker` as our way to declaratively request asynchronous work to be performed, wrapping the imperative API for an asynchronous task.

The `Worker` protocol requires two methods, `run` and `isEquivalent`. The `run` method must return a ReactiveSwift `SignalProducer` which is a cold observable that will start our asynchronous task when it is subscribed to:

```swift
public protocol Worker {

    /// The type of output events returned by this worker.
    associatedtype Output

    /// Returns a signal producer to execute the work represented by this worker.
    func run() -> SignalProducer<Output, NoError>

    /// Returns `true` if the other worker should be considered equivalent to `self`. Equivalence should take into
    /// account whatever data is meaninful to the task. For example, a worker that loads a user account from a server
    /// would not be equivalent to another worker with a different user ID.
    func isEquivalent(to otherWorker: Self) -> Bool
}
```

# TODO List populated from Github issues

Our TODO list has a single placeholder item in it, but it would be more useful to fetch a list of TODOs from a server (and ideally sync them back). We'll add support for the former, using the Workflow projects list of issues as our set of TODO items.

## Fetching Issues from the Github API

The [Github API](https://developer.github.com/v3/issues/#list-issues-for-a-repository) provides an unauthenticated endpoint for fetching the issue from a repo.

To fetch the issues from the workflow repo, we will use the URL: `https://api.github.com/repos/square/workflow/issues`

An example response from the time of writing this tutorial is (just looking at some fields we might want to show:

```json
[
  {
    "url": "https://api.github.com/repos/square/workflow/issues/605",
    ...snipped...
    "title": "Remove key from TypedWorker helpers",
    ...snipped...
    "body": "_Everybody_ is confused why this exists along with the key passed to `runningWorker`, I don't think I've seen anyone actually use this, and if you _do_ need this functionality it's just better to write a custom worker anyway."
  },
  {
    "url": "https://api.github.com/repos/square/workflow/issues/604",
    ...snipped...
    "title": "RenderTester should allow test to inspect which workers were all rendered",
    ...snipped...
    "body": ""
  }
]
```

We can use the swift [JSONDecoder](https://developer.apple.com/documentation/foundation/jsondecoder) to easily decode the JSON response into a defined type:

```swift
struct GithubIssue: Codable {
    var title: String
    var body: String
}

let decoder = JSONDecoder()
let issues = try decoder.decode([GithubIssue].self, from: data)
```

Define an `IssueService` to encapsulate the fetching and decoding of github issues:
```swift
// IssueService.swift
import Foundation
import ReactiveSwift
import Result


// HAX test JSON decoding
let data = """
        [
          {
            "url": "https://api.github.com/repos/square/workflow/issues/605",
            "title": "Remove key from TypedWorker helpers",
            "body": "_Everybody_ is confused why this exists along with the key passed to `runningWorker`, I don't think I've seen anyone actually use this, and if you _do_ need this functionality it's just better to write a custom worker anyway."
          },
          {
            "url": "https://api.github.com/repos/square/workflow/issues/604",
            "title": "RenderTester should allow test to inspect which workers were all rendered",
            "body": ""
          }
        ]
        """.data(using: .utf8)!

struct GithubIssue: Codable {
    var title: String
    var body: String
}

struct IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        let url = URL(string: "https://api.github.com/repos/square/workflow/issues")!
        let urlRequest = URLRequest(url: url)
        return URLSession.shared.reactive.data(with: urlRequest).attemptMap { arg in
            // TODO: Inspect the response before trying to decode.
            let (data, _) = arg

            let decoder = JSONDecoder()
            let issues = try decoder.decode([GithubIssue].self, from: data)
            return issues
        }
    }
}
```

Since the `Worker` API's `run` method must return a `SignalProducer`, we are using the reactive extensions to the foundation API provided by `ReactiveSwift` for our URLSession request.
