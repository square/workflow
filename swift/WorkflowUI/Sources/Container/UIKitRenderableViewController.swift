import UIKit
import ReactiveSwift


/// An implementation of `Renderable` that displays screens as view controllers.
internal final class UIKitRenderableViewController: UIViewController {

    private(set) internal var currentScreenViewController: AnyScreenViewController? = nil

    private let viewRegistry: ViewRegistry

    init(viewRegistry: ViewRegistry) {
        self.viewRegistry = viewRegistry
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
    }

    func render(screen: Screen) {

        if let currentScreenViewController = currentScreenViewController {
            if currentScreenViewController.screenType == type(of: screen) {
                currentScreenViewController.update(screen: screen)
            } else {
                currentScreenViewController.willMove(toParent: nil)
                currentScreenViewController.view.removeFromSuperview()
                currentScreenViewController.removeFromParent()
                self.currentScreenViewController = nil
            }
        }

        if currentScreenViewController == nil {
            let newMainScreen = screen.inflate(from: viewRegistry)
            addChild(newMainScreen)
            view.addSubview(newMainScreen.view)
            view.sendSubviewToBack(newMainScreen.view)
            newMainScreen.didMove(toParent: self)
            self.currentScreenViewController = newMainScreen

            self.setNeedsStatusBarAppearanceUpdate()
            if #available(iOS 11.0, *) {
                self.setNeedsUpdateOfHomeIndicatorAutoHidden()
                self.setNeedsUpdateOfScreenEdgesDeferringSystemGestures()
            }
        }

    }


    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        currentScreenViewController?.view.frame = view.bounds
    }

    public override var childForStatusBarStyle: UIViewController? {
        return currentScreenViewController
    }

    public override var childForStatusBarHidden: UIViewController? {
        return currentScreenViewController
    }

    override var childForHomeIndicatorAutoHidden: UIViewController? {
        return currentScreenViewController
    }

    override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return currentScreenViewController
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return currentScreenViewController?.supportedInterfaceOrientations ?? super.supportedInterfaceOrientations
    }

}
