# Building a View Controller from a Screen

Now that we have a workflow, we need a way to map our screen to an actual view controller.

## `ScreenViewController`

The `ScreenViewController` provides a base class that hides the plumbing of updating a view
controller from a view model update.

```swift
struct DemoScreen: Screen {
    let title: String
    let onTap: () -> Void

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return DemoScreenViewController.description(for: self, environment: environment)
    }
}


class DemoScreenViewController: ScreenViewController<DemoScreen> {

    private let button: UIButton

    required init(screen: DemoScreen, environment: ViewEnvironment) {
        button = UIButton()
        super.init(screen: screen, environment: environment)

        update(screen: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        button.addTarget(self, action: #selector(buttonPressed(sender:)), for: .touchUpInside)

        view.addSubview(button)
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        button.frame = view.bounds
    }

    override func screenDidChange(from previousScreen: DemoScreen, previousEnvironment: ViewEnvironment) {
        super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)
        update(screen: screen)
    }

    private func update(screen: DemoScreen) {
        button.setTitle(screen.title, for: .normal)
    }

    @objc private func buttonPressed(sender: UIButton) {
        screen.onTap()
    }

}
```

### Lifecycle

1. When the view controller is first created, it is given the initial screen value. In the example,
   we create the button and set the title for it via the `update` method.
1. The view loads as normal, adding the button the hierarchy and setting up the `target:action` for
   the button being pressed.
1. The button is tapped. When the callback is called, we call the `onTap` closure passed into the
   screen. The workflow will handle this event, update its state, and a new screen will be rendered.
1. The updated screen is passed to the view controller via the
   `screenDidChange(from previousScreen: previousEnvironment: previousEnvironment:)` method. Again,
   the view controller updates the title of the button based on what was passed in the screen.
