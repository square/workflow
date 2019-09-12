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


// Define the `IssueService` as a protocol so it can be mocked in tests.
protocol IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError>
}


struct GithubIssue: Codable {
    var title: String
    var body: String
}


// The real `IssueService` that will request the issue list from github.
struct RealIssueService: IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        let url = URL(string: "https://api.github.com/repos/square/workflow/issues")!
        let urlRequest = URLRequest(url: url)
        return URLSession.shared.reactive.data(with: urlRequest).attemptMap { arg in
            let (data, _) = arg

            let decoder = JSONDecoder()
            let issues = try decoder.decode([GithubIssue].self, from: data)
            return issues
        }
    }
}
```

Since the `Worker` API's `run` method must return a `SignalProducer`, we are using the reactive extensions to the foundation API provided by `ReactiveSwift` for our URLSession request.

## Loading Screen

We'll add a loading screen on login to fetch the list of outstanding issues.

Add a `LoadingScreen` and `LoadingWorkflow` using the templates:

```swift
import Workflow
import WorkflowUI


struct LoadingScreen: Screen {
    // The loading screen does not have any parameters, will just show "Loading..."
}


final class LoadingViewController: ScreenViewController<LoadingScreen> {
    let loadingLabel: UILabel

    required init(screen: LoadingScreen, viewRegistry: ViewRegistry) {
        loadingLabel = UILabel(frame: .zero)

        super.init(screen: screen, viewRegistry: viewRegistry)
        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        loadingLabel.text = "Loading..."
        loadingLabel.font = UIFont.systemFont(ofSize: 44.0)
        loadingLabel.textColor = .black
        loadingLabel.textAlignment = .center
        view.addSubview(loadingLabel)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        loadingLabel.frame = view.bounds
    }

    // ...rest of the implementation...
```

Define the output for the `LoadingWorkflow` to include the list of TODOs, and an action for when the load completes. The `Rendering` will always be a `LoadingScreen`:

```swift
// MARK: Input and Output

struct LoadingWorkflow: Workflow {
    // The issue service dependency is passed into the workflow.
    var issueService: IssueService

    enum Output {
        // Output when the todo's have finished loading.
        case loadCompleted([TodoModel])
        // Output if the load failed.
        case loadFailed(Error)
    }
}

// ...default state implementation...

// MARK: Actions

extension LoadingWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = LoadingWorkflow

        case loadCompleted([TodoModel])
        case loadFailed(Error)

        func apply(toState state: inout LoadingWorkflow.State) -> LoadingWorkflow.Output? {

            switch self {
            case .loadCompleted(let todos):
                return .loadCompleted(todos)

            case .loadFailed(let error):
                return .loadFailed(error)
            }

        }
    }
}

// MARK: Rendering

extension LoadingWorkflow {
    typealias Rendering = LoadingScreen

    func render(state: LoadingWorkflow.State, context: RenderContext<LoadingWorkflow>) -> String {
        return LoadingScreen()
    }
}
```

### Issue Loading Worker

Define an `IssueLoadingWorker` conforming to the `Worker` protocol in the `LoadingWorkflow`. The `Output` will be defined as the `Action` type for this workflow. The `run` method will map the result from the `fetchIssues` call into an action this workflow can receive:

```swift
// MARK: Workers

extension LoadingWorkflow {

    struct IssueLoadingWorker: Worker {

        // This worker will use the `LoadingWorkflow` `Action` as its output.
        typealias Output = Action

        // The worker needs a `IssueService` to request Github Issues.
        var issueService: IssueService

        func run() -> SignalProducer<Output, NoError> {
            // Fetch the issues, and map them from any array of `GithubIssue`s to `TodoModel`s.
            return issueService.fetchIssues().map { githubIssues in
                let todos = githubIssues.map { issue -> TodoModel in
                    return TodoModel(title: issue.title, note: issue.body)
                }

                // Return as a `.loadCompleted` action.
                return .loadCompleted(todos)
            }
            .flatMapError { error in
                // As the SignalProducer must return a `NoError`, flat map the error to our failure action.
                SignalProducer(value: .loadFailed(error))
            }
        }

        // Always consider two workers equivalent.
        func isEquivalent(to otherWorker: IssueLoadingWorker) -> Bool {
            return true
        }

    }

}
```

To request our worker to be run, we specify it in the `render` method with a call to `awaitResult`. The infrastructure will ensure that a single `IssueLoadingWorker` is running even across multiple render passes:

```swift
// MARK: Rendering

extension LoadingWorkflow {
    typealias Rendering = LoadingScreen

    func render(state: LoadingWorkflow.State, context: RenderContext<LoadingWorkflow>) -> Rendering {

        // Request that the `IssueLoadingWorker` be run if it has not been started. When it outputs, the action
        // returned from it will be applied.
        context.awaitResult(for: IssueLoadingWorker(issueService: issueService))

        return LoadingScreen()
    }
}
```

## Populating the TODO list from Github issues

### Loading as a step in the TodoWorkflow

We will add the loading screen as the first step in the `TodoWorkflow`:

```swift
// MARK: Input and Output

struct TodoWorkflow: Workflow {
    var name: String
    // Have the `IssueService` be provided as a dependency to the `TodoWorkflow`:
    var issueService: IssueService

    enum Output {
        case back
    }
}


// MARK: State and Initialization

extension TodoWorkflow {

    struct State: Equatable {
        var todos: [TodoModel]
        var step: Step
        enum Step: Equatable {
            // Show a loading screen while fetching the initial set of TODO items
            case loading
            // Showing the list of todo items.
            case list
            // Editing a single item. The state holds the index so it can be updated when a save action is received.
            case edit(index: Int)
        }
    }

    func makeInitialState() -> TodoWorkflow.State {
        return State(
            todos: [],
            // Start from the `.loading` step, which will show our loading screen and fetch the initial list.
            step: .loading)
    }

    func workflowDidChange(from previousWorkflow: TodoWorkflow, state: inout State) {

    }
}

// ...rest of the implementation...
```

Add a new `Action` type for the loading success or failure:

```swift
// MARK: Actions

extension TodoWorkflow {

    enum LoadAction: WorkflowAction {
        typealias WorkflowType = TodoWorkflow

        case loaded(todos: [TodoModel])
        case loadingFailed(Error)

        func apply(toState state: inout State) -> Output? {
            switch self {

            case .loaded(todos: let todos):
                // Populate the `todos` from a successful load.
                state.todos = todos
                state.step = .list
                return nil

            case .loadingFailed:
                // For now, just go back if we fail to load the issues.
                // We could also consider showing a default TODO if this fails, or an error message, etc.
                return .back
            }
        }
    }

// ...rest of the implementation...
```

Then, update the `render` method to show the screen from the `LoadingWorkflow` when the step is `.loading`:

```swift
// MARK: Rendering

extension TodoWorkflow {

    typealias Rendering = [BackStackScreen.Item]

    func render(state: TodoWorkflow.State, context: RenderContext<TodoWorkflow>) -> Rendering {

        let todoListItem = TodoListWorkflow(
            name: name,
            todos: state.todos)
            .mapOutput({ output -> ListAction in
                switch output {

                case .back:
                    return .back

                case .selectTodo(index: let index):
                    return .editTodo(index: index)

                case .newTodo:
                    return .newTodo
                }
            })
            .rendered(with: context)

        switch state.step {
        // Add a case for the loading step:
        case .loading:
            let loadingScreen = LoadingWorkflow(issueService: issueService)
                // Map the output of the LoadingWorkflow to our LoadAction.
                .mapOutput({ output -> LoadAction in
                    switch output {
                    case .loadCompleted(let todos):
                        return .loaded(todos: todos)
                    case .loadFailed(let error):
                        return .loadingFailed(error)
                    }
                })
                .rendered(with: context)
            return [
                BackStackScreen.Item(
                    screen: loadingScreen,
                    barVisibility: .hidden)
            ]

        case .list:
            // Return only the list item.
            return [todoListItem]

        case .edit(index: let index):

            let todoEditItem = TodoEditWorkflow(
                initialTodo: state.todos[index])
                .mapOutput({ output -> EditAction in
                    switch output {
                    case .discard:
                        return .discardChanges

                    case .save(let updatedTodo):
                        return .saveChanges(index: index, todo: updatedTodo)
                    }
                })
                .rendered(with: context)

            // Return both the list item and edit.
            return [todoListItem, todoEditItem]
        }

    }
}
```

### Dependency injection of the Issue Service

The `IssueService` is be needed by the `LoadingWorkflow`, and transitively by the `TodoWorkflow`. We'll inject it as a dependency starting from the `RootWorkflow`, allowing the creator of our workflow tree to decide to use the real or a mock version of the `IssueService`:

```swift
// MARK: Input and Output

struct RootWorkflow: Workflow {
    var issueService: IssueService

    enum Output {

    }
}

// ...rest of implementation...
// MARK: Rendering

extension RootWorkflow {

    typealias Rendering = BackStackScreen

    func render(state: RootWorkflow.State, context: RenderContext<RootWorkflow>) -> Rendering {
        // ...snipped...

        switch state {
        // When the state is `.welcome`, defer to the WelcomeWorkflow.
        case .welcome:
            // We always add the welcome screen to the backstack, so this is a no op.
            break

        // When the state is `.todo`, defer to the TodoListWorkflow.
        case .todo(name: let name):

            // The `issueService` is passed to the `TodoWorkflow`:
            let todoBackStackItems = TodoWorkflow(name: name, issueService: issueService)
                .mapOutput({ output -> Action in
                    switch output {
                    case .back:
                        // When receiving a `.back` output, treat it as a `.logout` action.
                        return .logout
                    }
                })
                .rendered(with: context)

            backStackItems.append(contentsOf: todoBackStackItems)
        }

        // Finally, return the BackStackScreen with a list of BackStackScreen.Items
        return BackStackScreen(items: backStackItems)
    }
}

```

Finally, we'll update the `TutorialContainerViewController`, registering the `LoadingScreen` and passing in a `RealIssueService` to the `RootWorkflow`:

```swift
public final class TutorialContainerViewController: UIViewController {
    let containerViewController: UIViewController

    public init() {
        // Create a view registry. This will allow the infrastructure to map `Screen` types to their respective view controller type.
        var viewRegistry = ViewRegistry()
        // Register the `WelcomeScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerWelcomeScreen()
        // Register the `TodoListScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerTodoListScreen()
        // Register the `BackStackContainer`, which provides a container for the `BackStackScreen`.
        viewRegistry.registerBackStackContainer()
        // Register the `TodoEditScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerTodoEditScreen()
        // Register the `LoadingScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerLoadingScreen()

        // Create a `ContainerViewController` with the `RootWorkflow` as the root workflow, with the view registry we just created.
        containerViewController = ContainerViewController(
            // Create a `RealIssueService` and pass it to the `RootWorkflow`.
            workflow: RootWorkflow(issueService: RealIssueService()),
            viewRegistry: viewRegistry)

        super.init(nibName: nil, bundle: nil)
    }
// ...rest of the implementation...
```

Run the app again - now, after providing a name, our loading screen will be shown and then the TODO list will be populated from the open Github issues.

## Testing

### Fixing up the existing tests

Our tests will no longer build, as two of our workflows have been updated to take an `IssueService` as a property. Create a `TestFixtures` file in the tutorial unit testing target and add a `FakeIssueService`:

```swift
// TestFixtures.swift

import ReactiveSwift
import Result
@testable import Tutorial6


struct FakeIssueService: IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        fatalError()
    }
}
```

And update the `RootWorkflowTests` and `TodoWorkflowTests` to have a `FakeIssueService` passed into those workflows.

However, the `testAppFlow` test in `RootWorkflowTests` will now fail. Why? Because we've added an extra screen that will be displayed, and also because our `FakeIssueService` will fatal error if it's called. Since this is an integration test, we'll need to have a mock version of the `IssueService` so it can pass:

```swift
// TestFixtures.swift

struct MockIssueService: IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        let issues = [GithubIssue(title: "Issue Title", body: "Issue Body")]

        return SignalProducer(value: issues)
    }
}
```

And use it in the `testAppFlow` test:

```swift
// RootWorkflowTests.swift
    func testAppFlow() {

        let workflowHost = WorkflowHost(workflow: RootWorkflow(issueService: MockIssueService()))

        // First rendering is just the welcome screen. Update the name.
        do {
            let backStack = workflowHost.rendering.value
            XCTAssertEqual(1, backStack.items.count)

            guard let welcomeScreen = backStack.items[0].screen.wrappedScreen as? WelcomeScreen else {
                XCTFail("Expected initial screen of `WelcomeScreen`")
                return
            }

            welcomeScreen.onNameChanged("MyName")
        }

        // Log in and go to the welcome list
        do {
            let backStack = workflowHost.rendering.value
            XCTAssertEqual(1, backStack.items.count)

            guard let welcomeScreen = backStack.items[0].screen.wrappedScreen as? WelcomeScreen else {
                XCTFail("Expected initial screen of `WelcomeScreen`")
                return
            }

            welcomeScreen.onLoginTapped()
        }

        // Expect the loading screen.
        do {
            let backStack = workflowHost.rendering.value
            XCTAssertEqual(2, backStack.items.count)

            guard let _ = backStack.items[0].screen.wrappedScreen as? WelcomeScreen else {
                XCTFail("Expected first screen of `WelcomeScreen`")
                return
            }

            guard let _ = backStack.items[1].screen.wrappedScreen as? LoadingScreen else {
                XCTFail("Expected second screen of `LoadingScreen`")
                return
            }
        }

        let expectation = XCTestExpectation(description: "Todo List Screen Shown")
        let disposable = workflowHost.rendering.signal.observeValues { rendering in
            guard let _ = rendering.items[1].screen.wrappedScreen as? TodoListScreen else {
                return
            }
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
        disposable?.dispose()

        // Expect the todo list. Edit the first todo.
        do {
            let backStack = workflowHost.rendering.value
            XCTAssertEqual(2, backStack.items.count)

            guard let _ = backStack.items[0].screen.wrappedScreen as? WelcomeScreen else {
                XCTFail("Expected first screen of `WelcomeScreen`")
                return
            }

            guard let todoScreen = backStack.items[1].screen.wrappedScreen as? TodoListScreen else {
                XCTFail("Expected second screen of `TodoListScreen`")
                return
            }
            XCTAssertEqual(1, todoScreen.todoTitles.count)
            // Select the first todo:
            todoScreen.onTodoSelected(0)
        }

        //...rest of the test...
```

Unlike previously, we had to explicitly wait for the output from the `LoadingWorkflow` to be handled before checking for the `TodoListScreen`. Generally, having integration tests in this style are less easy to write, and have a lot more moving parts, so they're not as recommended as testing individual workflows or doing full UI tests.

### Testing the LoadingWorkflow

Now, add a `LoadingWorkflowTests` to have unit test coverage of the `LoadingWorkflow`:

```swift
```
