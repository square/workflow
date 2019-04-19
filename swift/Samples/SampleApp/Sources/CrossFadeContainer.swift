import Workflow
import WorkflowUI


struct CrossFadeScreen: Screen {
    var baseScreen: AnyScreen
    var key: AnyHashable

    init<ScreenType: Screen, Key: Hashable>(base screen: ScreenType, key: Key?) {
        self.baseScreen = AnyScreen(screen)
        if let key = key {
            self.key = AnyHashable(key)
        } else {
            self.key = AnyHashable(ObjectIdentifier(ScreenType.self))
        }
    }

    init<ScreenType: Screen>(base screen: ScreenType) {
        let key = Optional<AnyHashable>.none
        self.init(base: screen, key: key)
    }

    fileprivate func isEquivalent(to otherScreen: CrossFadeScreen) -> Bool {
        return self.key == otherScreen.key
    }
}


extension ViewRegistry {

    public mutating func registerCrossFadeContainer() {
        self.register(screenViewControllerType: CrossFadeContainerViewController.self)
    }

}


fileprivate final class CrossFadeContainerViewController: ScreenViewController<CrossFadeScreen> {
    var childViewController: ScreenViewController<AnyScreen>

    required init(screen: CrossFadeScreen, viewRegistry: ViewRegistry) {
        childViewController = viewRegistry.provideView(for: screen.baseScreen)
        super.init(screen: screen, viewRegistry: viewRegistry)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(childViewController)
        view.addSubview(childViewController.view)
        childViewController.didMove(toParent: self)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        childViewController.view.frame = view.bounds
    }

    override func screenDidChange(from previousScreen: CrossFadeScreen) {
        if screen.isEquivalent(to: previousScreen) {
            childViewController.update(screen: screen.baseScreen)
        } else {
            // The new screen is different than the previous. Animate the transition.
            let oldChild = childViewController
            childViewController = viewRegistry.provideView(for: screen.baseScreen)
            addChild(childViewController)
            view.addSubview(childViewController.view)
            UIView.transition(
                from: oldChild.view,
                to: childViewController.view,
                duration: 0.72,
                options: .transitionCrossDissolve,
                completion: { [childViewController] completed in
                    childViewController.didMove(toParent: self)

                    oldChild.willMove(toParent: nil)
                    oldChild.view.removeFromSuperview()
                    oldChild.removeFromParent()
                })
        }
    }

}
