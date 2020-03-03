# Using a workflow to show UI

## `ContainerViewController`

In the Workflow architecture, the container acts as the glue between the state-driven world of
Workflows and the UI that is ultimately displayed. On iOS, the container is implemented as
`ContainerViewController`.

```swift

/// Drives view controllers from a root Workflow.
public final class ContainerViewController<Output, ScreenType>: UIViewController where ScreenType: Screen {

    /// Emits output events from the bound workflow.
    public let output: Signal<Output, Never>

    public convenience init<W: Workflow>(workflow: W) where W.Rendering == ScreenType, W.Output == Output
}

```

The initializer argument is the workflow that will drive your application.

```swift
import UIKit
import Workflow
import WorkflowUI

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)

        let container = ContainerViewController(
            workflow: DemoWorkflow()
        )

        window.rootViewController = container
        self.window = window
        window.makeKeyAndVisible()
        return true
    }
}

```

Now, when the `ContainerViewController` is shown, it will start the workflow and `render` will be
called returning the `DemoScreen`. The container will use `viewControllerDescription` to build
a `DemoScreenViewController` and add it to the view hierarchy to display.
