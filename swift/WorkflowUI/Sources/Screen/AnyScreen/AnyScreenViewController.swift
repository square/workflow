internal final class AnyScreenViewController: ScreenViewController<AnyScreen> {

    typealias WrappedViewController = UIViewController & UntypedScreenViewController

    private (set) internal var currentViewController: WrappedViewController

    required init(screen: AnyScreen, viewRegistry: ViewRegistry) {
        currentViewController = screen.makeViewController(from: viewRegistry)
        super.init(screen: screen, viewRegistry: viewRegistry)

        addChild(currentViewController)
        currentViewController.didMove(toParent: self)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(currentViewController.view)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        currentViewController.view.frame = view.bounds
    }

    override func screenDidChange(from previousScreen: AnyScreen) {
        super.screenDidChange(from: previousScreen)

        if type(of: screen.wrappedScreen) == currentViewController.screenType {
            currentViewController.update(untypedScreen: screen.wrappedScreen)
        } else {
            currentViewController.willMove(toParent: nil)
            if isViewLoaded {
                currentViewController.view.removeFromSuperview()
            }
            currentViewController.removeFromParent()

            currentViewController = screen.makeViewController(from: viewRegistry)
            addChild(currentViewController)
            if isViewLoaded {
                view.addSubview(currentViewController.view)
            }
            currentViewController.didMove(toParent: self)
        }
    }

    public override var childForStatusBarStyle: UIViewController? {
        return currentViewController
    }

    public override var childForStatusBarHidden: UIViewController? {
        return currentViewController
    }

    public override var childForHomeIndicatorAutoHidden: UIViewController? {
        return currentViewController
    }

    public override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return currentViewController
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return currentViewController.supportedInterfaceOrientations
    }

    

}



