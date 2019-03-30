# Building a View Controller from a Screen

Now that we have a workflow, we need a way to map our screen to an actual view controller.


## `ScreenViewController`

The `ScreenViewController` provides a baseclass the hides the plumbing of updating a view controller from a view model update.

```swift
struct DemoScreen: Screen {
    let title: String
    let onTap: () -> Void
}


class DemoScreenViewController: ScreenViewController<SimpleScreen> {

    private let button: UIButton

    required init(screen: SimpleScreen, viewRegistry: ViewRegistry) {
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

    override func screenDidChange(from previousScreen: SimpleScreen) {
        super.screenDidChange(from: previousScreen)
        update(screen: screen)
    }

    private func update(screen: SimpleScreen) {
        button.setTitle(screen.title, for: .normal)
    }

    @objc private func buttonPressed(sender: UIButton) {
        screen.onTap()
    }

}
```
