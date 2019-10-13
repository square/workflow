public final class AnyScreenViewController: UIViewController {

    private(set) public var screenType: Any.Type
    private(set) internal var wrappedViewController: UIViewController

    internal init<S: Screen>(wrappedScreen: S) {
        screenType = S.self
        wrappedViewController = wrappedScreen.makeViewController()
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    internal func update<S: Screen>(from wrappedScreen: S) {
        if let wrappedViewController = wrappedViewController as? S.ViewController {
            // Possible to have a screen
            screenType = S.self
            wrappedScreen.update(viewController: wrappedViewController)
        } else {
            // do view controller containment wrappedViewController
            screenType = S.self
            wrappedViewController = wrappedScreen.makeViewController()
        }
    }

//    internal struct TypedViewController {
//        let screenType: Any.Type
//        let viewController: UIViewController
//
//        init<S: Screen>(screen: S) {
//            screenType = S.self
//            viewController = screen.makeViewController()
//        }
//
//    }

//    private(set) internal var currentViewController: TypedViewController
//    private(set) internal var currentViewController: UIViewController
//
//    typealias WrappedViewController = UIViewController & UntypedScreenViewController
//
//    private (set) internal var currentViewController: WrappedViewController

//    required init(screen: AnyScreen) {
//        currentViewController = TypedViewController(screen: screen)
////        currentViewController = screen.makeViewController()
//        super.init(screen: screen, viewRegistry: viewRegistry)
//
//        addChild(currentViewController)
//        currentViewController.didMove(toParent: self)
//    }
//
//    override func viewDidLoad() {
//        super.viewDidLoad()
//        view.addSubview(currentViewController.view)
//    }
//
//    override func viewDidLayoutSubviews() {
//        super.viewDidLayoutSubviews()
//        currentViewController.view.frame = view.bounds
//    }
//
//    override func screenDidChange(from previousScreen: AnyScreen) {
//        super.screenDidChange(from: previousScreen)
//
//        if type(of: screen.wrappedScreen) == currentViewController.screenType {
//            currentViewController.update(untypedScreen: screen.wrappedScreen)
//        } else {
//            currentViewController.willMove(toParent: nil)
//            if isViewLoaded {
//                currentViewController.view.removeFromSuperview()
//            }
//            currentViewController.removeFromParent()
//
//            currentViewController = screen.makeViewController(from: viewRegistry)
//            addChild(currentViewController)
//            if isViewLoaded {
//                view.addSubview(currentViewController.view)
//            }
//            currentViewController.didMove(toParent: self)
//        }
//    }
//
//    public override var childForStatusBarStyle: UIViewController? {
//        return currentViewController
//    }
//
//    public override var childForStatusBarHidden: UIViewController? {
//        return currentViewController
//    }
//
//    public override var childForHomeIndicatorAutoHidden: UIViewController? {
//        return currentViewController
//    }
//
//    public override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
//        return currentViewController
//    }
//
//    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
//        return currentViewController.supportedInterfaceOrientations
//    }

    

}



