# Building a View Controller from a Screen

Now that we have a workflow, we need a way to map our screen to an actual view controller.


## `ScreenViewController`

The `ScreenViewController` provides a base class that hides the plumbing of updating a view controller from a view model update.

```swift
struct DemoScreen: Screen {
    let title: String
    let onTap: () -> Void
}


class DemoScreenViewController: ScreenViewController<DemoScreen> {

    private let button: UIButton

    required init(screen: DemoScreen, viewRegistry: ViewRegistry) {
        button = UIButton()
        super.init(screen: screen, viewRegistry: viewRegistry)

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

    override func screenDidChange(from previousScreen: DemoScreen) {
        super.screenDidChange(from: previousScreen)
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

1. When the view controller is first created, it is given the initial screen value. In the example, we create the button and set the title for it via the `update` method.
2. The view loads as normal, adding the button the hierarchy and setting up the `target:action` for the button being pressed.
3. The button is tapped. When the callback is called, we call the `onTap` closure passed into the screen. The workflow will handle this event, update its state, and a new screen will be rendered.
4. The updated screen is passed to the view controller via the `screenDidChange(from previousScreen:)` method. Again, the view controller updates the title of the button based on what was passed in the screen.
