# Using a workflow to show UI


## `ContainerViewController`

In the Workflow architecture, the container acts as the glue between the state-driven world of Workflows and the UI that is ultimately displayed. On iOS, the container is implemented as `ContainerViewController`.

```swift

/// Drives view controllers from a root Workflow.
public final class ContainerViewController<Output, ScreenType>: UIViewController where ScreenType: Screen {

    /// Emits output events from the bound workflow.
    public let output: Signal<Output, NoError>

    public convenience init<W: Workflow>(workflow: W, viewRegistry: ViewRegistry) where W.Rendering == ScreenType, W.Output == Output
}

```

The first initializer argument is the workflow that will drive your application.

The second initializer argument is the view registry. The view registry acts as a mapping between the view models (`Screen`s) that your workflow emits and the concrete UI implementations that should be used to display them.

```swift
import UIKit
import Workflow
import WorkflowUI

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)

        var viewRegistry = ViewRegistry()

        let container = ContainerViewController(
            workflow: DemoWorkflow(),
            viewRegistry: viewRegistry)

        window.rootViewController = container
        self.window = window
        window.makeKeyAndVisible()
        return true
    }
}

```

Your project should compile at this point. It will crash as soon as the workflow emits a screen, however, because we have not registered any UI implementations with the view registry. Let's fix that:

```swift
var viewRegistry = ViewRegistry()

// Register the DemoScreenViewController to be responsible for DemoScreen.
viewRegistry.register(screenViewControllerType: DemoScreenViewController.self)

let container = ContainerViewController(
    workflow: DemoWorkflow(),
    viewRegistry: viewRegistry)
```

Now, when the `ContainerViewController` is shown, it will start the workflow and `compose` will be called returning the `DemoScreen`. The container will use the view registry to map the `DemoScreen` to a `DemoScreenViewController` and add it to the view hierarchy to display.
